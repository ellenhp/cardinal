use std::{
    collections::{BTreeMap, HashMap},
    sync::{Arc, Mutex},
};

use tantivy::{
    TantivyDocument, Term,
    collector::TopDocs,
    directory::MmapDirectory,
    query::{PhraseQuery, Query, QueryParser, TermQuery},
    schema::{
        INDEXED, IndexRecordOption, OwnedValue, STORED, Schema, SchemaBuilder, TextFieldIndexing,
        TextOptions, Value,
    },
    tokenizer::{Language, LowerCaser, SimpleTokenizer, Stemmer, TextAnalyzer},
};

use crate::error::AirmailError;
use crate::poi::{InputPoi, PointOfInterest, SchemafiedPoi};

fn all_subsequences(tokens: &[String]) -> Vec<Vec<String>> {
    let mut subsequences: Vec<Vec<String>> = Vec::new();
    for i in 0..tokens.len() {
        for j in i..tokens.len() {
            subsequences.push(tokens[i..=j].iter().map(|s| s.to_string()).collect());
        }
    }
    subsequences
}

#[derive(uniffi::Object)]
pub struct AirmailIndex {
    index: tantivy::Index,
    // I'm going to hell and/or prison for this, but we can't mutate `self` in a uniffi-exposed function.
    // Moreover, we need to mutate the IndexWriter itself so I think we need a mutex there too.
    writer: Mutex<Option<Arc<Mutex<tantivy::IndexWriter>>>>,
    lang: String,
}

static FIELD_S2CELL: &str = "s2cell";
static FIELD_S2CELL_PARENTS: &str = "s2cell_parents";
static FIELD_CONTENT: &str = "content";
static FIELD_TAGS: &str = "tags";

impl AirmailIndex {
    fn schema(lang: &str) -> Schema {
        let text_options = TextOptions::default().set_indexing_options(
            TextFieldIndexing::default()
                .set_index_option(IndexRecordOption::WithFreqsAndPositions)
                .set_tokenizer(&lang),
        );

        let mut builder = SchemaBuilder::new();
        builder.add_u64_field(FIELD_S2CELL, INDEXED | STORED);
        builder.add_u64_field(FIELD_S2CELL_PARENTS, INDEXED);
        builder.add_text_field(FIELD_CONTENT, text_options);
        builder.add_json_field(FIELD_TAGS, STORED);
        builder.build()
    }

    fn field_s2cell(&self) -> tantivy::schema::Field {
        Self::schema(&self.lang)
            .get_field(FIELD_S2CELL)
            .expect("Field not found")
    }

    fn field_s2cell_parents(&self) -> tantivy::schema::Field {
        Self::schema(&self.lang)
            .get_field(FIELD_S2CELL_PARENTS)
            .expect("Field not found")
    }

    fn field_content(&self) -> tantivy::schema::Field {
        Self::schema(&self.lang)
            .get_field(FIELD_CONTENT)
            .expect("Field not found")
    }

    fn field_tags(&self) -> tantivy::schema::Field {
        Self::schema(&self.lang)
            .get_field(FIELD_TAGS)
            .expect("Field not found")
    }

    pub fn new_in_ram(lang: &str) -> AirmailIndex {
        let index = tantivy::Index::create_in_ram(AirmailIndex::schema(lang));

        let tokenizers = index.tokenizers();

        let stemmer_lang = match lang {
            "ar" => Some(Language::Arabic),
            "da" => Some(Language::Danish),
            "nl" => Some(Language::Dutch),
            "en" => Some(Language::English),
            "fi" => Some(Language::Finnish),
            "fr" => Some(Language::French),
            "de" => Some(Language::German),
            "el" => Some(Language::Greek),
            "hu" => Some(Language::Hungarian),
            "it" => Some(Language::Italian),
            "no" => Some(Language::Norwegian),
            "pt" => Some(Language::Portuguese),
            "ro" => Some(Language::Romanian),
            "ru" => Some(Language::Russian),
            "es" => Some(Language::Spanish),
            "sv" => Some(Language::Swedish),
            "ta" => Some(Language::Tamil),
            "tr" => Some(Language::Turkish),
            _ => None,
        };

        let tokenizer = if let Some(stemmer_lang) = stemmer_lang {
            TextAnalyzer::builder(SimpleTokenizer::default())
                .filter(LowerCaser)
                .filter(Stemmer::new(stemmer_lang))
                .build()
        } else {
            TextAnalyzer::builder(SimpleTokenizer::default())
                .filter(LowerCaser)
                .build()
        };
        tokenizers.register(lang, tokenizer);

        AirmailIndex {
            index,
            lang: lang.to_string(),
            writer: Mutex::new(None),
        }
    }

    pub fn new_in_dir(lang: &str, dir: &str) -> Result<AirmailIndex, AirmailError> {
        let mmap_dir = MmapDirectory::open(dir)?;
        let index = tantivy::Index::open_or_create(mmap_dir, AirmailIndex::schema(lang))?;

        let tokenizers = index.tokenizers();

        let stemmer_lang = match lang {
            "ar" => Some(Language::Arabic),
            "da" => Some(Language::Danish),
            "nl" => Some(Language::Dutch),
            "en" => Some(Language::English),
            "fi" => Some(Language::Finnish),
            "fr" => Some(Language::French),
            "de" => Some(Language::German),
            "el" => Some(Language::Greek),
            "hu" => Some(Language::Hungarian),
            "it" => Some(Language::Italian),
            "no" => Some(Language::Norwegian),
            "pt" => Some(Language::Portuguese),
            "ro" => Some(Language::Romanian),
            "ru" => Some(Language::Russian),
            "es" => Some(Language::Spanish),
            "sv" => Some(Language::Swedish),
            "ta" => Some(Language::Tamil),
            "tr" => Some(Language::Turkish),
            _ => None,
        };

        let tokenizer = if let Some(stemmer_lang) = stemmer_lang {
            TextAnalyzer::builder(SimpleTokenizer::default())
                .filter(LowerCaser)
                .filter(Stemmer::new(stemmer_lang))
                .build()
        } else {
            TextAnalyzer::builder(SimpleTokenizer::default())
                .filter(LowerCaser)
                .build()
        };
        tokenizers.register(lang, tokenizer);

        Ok(AirmailIndex {
            index,
            lang: lang.to_string(),
            writer: Mutex::new(None),
        })
    }

    fn search_inner(&self, query: Box<dyn Query>) -> Result<Vec<PointOfInterest>, AirmailError> {
        let reader = self.index.reader()?;
        let searcher = reader.searcher();
        let top_docs = searcher.search(&query, &TopDocs::with_limit(10))?;

        let results = top_docs
            .into_iter()
            .flat_map(|(_score, doc)| {
                let doc: TantivyDocument = searcher.doc(doc).ok()?;
                let s2cell = doc.get_first(self.field_s2cell())?.as_u64()?;
                let cellid = s2::cellid::CellID(s2cell);
                let latlng = s2::latlng::LatLng::from(cellid);
                let tags: Vec<(String, String)> = doc
                    .get_first(self.field_tags())?
                    .as_object()?
                    .map(|(k, v)| (k.to_string(), v.as_str().unwrap_or_default().to_string()))
                    .collect();

                Some(PointOfInterest::new(
                    latlng.lat.deg(),
                    latlng.lng.deg(),
                    tags,
                ))
            })
            .collect::<Vec<_>>();

        Ok(results)
    }
}

#[uniffi::export]
impl AirmailIndex {
    pub fn search_phrase(&self, query: &str) -> Result<Vec<PointOfInterest>, AirmailError> {
        let mut tokenizer = self.index.tokenizer_for_field(self.field_content())?;
        let mut token_stream = tokenizer.token_stream(query);
        let mut tokens = vec![token_stream.token().text.clone()];
        while token_stream.advance() {
            tokens.push(token_stream.token().text.clone())
        }
        let all_phrase_queries: Vec<Box<dyn Query>> = all_subsequences(&tokens)
            .iter()
            .map(|seq| {
                if seq.len() > 1 {
                    let terms: Vec<Term> = seq
                        .iter()
                        .map(|term| Term::from_field_text(self.field_content(), &term))
                        .collect();
                    let b: Box<dyn Query> = Box::new(PhraseQuery::new(terms));
                    b
                } else {
                    Box::new(TermQuery::new(
                        Term::from_field_text(self.field_content(), &seq[0]),
                        IndexRecordOption::WithFreqsAndPositions,
                    ))
                }
            })
            .collect();
        let query = tantivy::query::BooleanQuery::union(all_phrase_queries);
        dbg!(&query);
        self.search_inner(Box::new(query))
    }

    pub fn search_raw(&self, query: &str) -> Result<Vec<PointOfInterest>, AirmailError> {
        let query_parser = QueryParser::for_index(&self.index, vec![self.field_content()]);
        let query = query_parser.parse_query(query)?;
        self.search_inner(query)
    }

    pub fn begin_ingestion(&self) -> Result<(), AirmailError> {
        *self.writer.lock().expect("Failed to lock writer mutex") = Some(Arc::new(Mutex::new(
            self.index
                .writer(15_000_000)
                .map_err(|err| AirmailError::Tantivy(err))?,
        )));
        Ok(())
    }

    pub fn ingest_tile_with_coordinates(
        &self,
        mvt: Vec<u8>,
        tile_x: u32,
        tile_y: u32,
        tile_z: u8,
    ) -> Result<u64, AirmailError> {
        // Clone the writer outside the lock to avoid holding the lock while doing work
        let writer = {
            let lock = self.writer.lock().expect("Failed to lock writer mutex");
            if let Some(writer) = lock.as_ref() {
                writer.clone()
            } else {
                return Err(AirmailError::InvalidIngestionState);
            }
        };

        let mut count = 0usize;
        let alphabetic_regex = regex::Regex::new("[a-z]+")?;
        let schema = AirmailIndex::schema(&self.lang);
        let reader = mvt_reader::Reader::new(mvt)?;
        let layers = reader.get_layer_names()?;
        if let Some((poi_layer_id, _)) = layers
            .iter()
            .enumerate()
            .filter(|(_, layer)| layer.as_str() == Some("poi"))
            .next()
        {
            let extent = reader.get_layer_metadata()?[poi_layer_id].extent;
            let features = reader.get_features(poi_layer_id)?;

            for feature in &features {
                let mut tags = HashMap::new();
                for (key, value) in feature.properties.as_ref().unwrap_or(&HashMap::new()) {
                    match value {
                        mvt_reader::feature::Value::String(value) => {
                            tags.insert(key.clone(), value.clone())
                        }
                        _ => continue,
                    };
                }
                if let Some(poi) = InputPoi::from_tags_with_transform(
                    &self.lang,
                    feature.get_geometry().clone(),
                    &tags,
                    tile_x,
                    tile_y,
                    tile_z,
                    extent,
                ) {
                    let poi: SchemafiedPoi = poi.into();

                    let mut doc = TantivyDocument::default();
                    for content in poi.content {
                        doc.add_text(schema.get_field(FIELD_CONTENT).unwrap(), content);
                    }
                    for (k, v) in &tags {
                        if alphabetic_regex.is_match(&k) && alphabetic_regex.is_match(&v) {
                            doc.add_text(
                                schema.get_field(FIELD_CONTENT).unwrap(),
                                format!("{k}={v}"),
                            );
                        }
                    }
                    doc.add_object(
                        self.field_tags(),
                        poi.tags
                            .iter()
                            .map(|(k, v)| (k.to_string(), OwnedValue::Str(v.to_string())))
                            .collect::<BTreeMap<String, OwnedValue>>(),
                    );
                    doc.add_u64(self.field_s2cell(), poi.s2cell);
                    for parent in poi.s2cell_parents {
                        doc.add_u64(self.field_s2cell_parents(), parent);
                    }

                    // Acquire the inner lock only when needed
                    writer
                        .lock()
                        .expect("Failed to lock writer mutex")
                        .add_document(doc)?;
                    count += 1;
                }
            }
        }
        Ok(count as u64)
    }

    pub fn commit_ingestion(&self) -> Result<(), AirmailError> {
        // Clone the writer outside the lock to avoid holding the lock while doing work
        let writer = {
            let lock = self.writer.lock().expect("Failed to lock writer mutex");
            if let Some(writer) = lock.as_ref() {
                writer.clone()
            } else {
                return Err(AirmailError::InvalidIngestionState);
            }
        };

        // Acquire the inner lock only when needed
        writer
            .lock()
            .expect("Failed to lock writer mutex")
            .commit()?;

        // Release the writer
        *self.writer.lock().expect("Failed to lock writer mutex") = None;
        Ok(())
    }
}

#[uniffi::export]
pub fn new_airmail_index(lang: String, storage_dir: String) -> Result<AirmailIndex, AirmailError> {
    AirmailIndex::new_in_dir(&lang, &storage_dir)
}

#[cfg(test)]
mod test {
    use std::time::Instant;

    use crate::index::AirmailIndex;

    #[test]
    pub fn test_find_lighthouse_roasters() {
        let index = AirmailIndex::new_in_ram("en");

        let start = Instant::now();
        index.begin_ingestion().unwrap();
        let count = index
            .ingest_tile_with_coordinates(
                include_bytes!("../testdata/z14.pbf").to_vec(),
                2624,
                5718,
                14,
            )
            .expect("Failed to ingest tile.");
        index.commit_ingestion().unwrap();

        dbg!(count);
        dbg!(start.elapsed());
        let results = dbg!(
            index
                .search_phrase("lighthouse roasters")
                .expect("Failed to execute search")
        );
        assert_eq!(results.len(), 1);
    }

    #[test]
    pub fn test_abbreviations() {
        let index = AirmailIndex::new_in_ram("en");

        let start = Instant::now();
        index.begin_ingestion().unwrap();
        let count = index
            .ingest_tile_with_coordinates(
                include_bytes!("../testdata/z14.pbf").to_vec(),
                2624,
                5718,
                14,
            )
            .expect("Failed to ingest tile.");
        index.commit_ingestion().unwrap();

        dbg!(count);
        dbg!(start.elapsed());
        let results = dbg!(
            index
                .search_phrase("400 n 43rd")
                .expect("Failed to execute search")
        );
        assert_eq!(
            results.first().unwrap().tag("name").unwrap(),
            "Lighthouse Roasters"
        )
    }

    #[test]
    pub fn test_stemming() {
        let index = AirmailIndex::new_in_ram("en");

        let start = Instant::now();
        index.begin_ingestion().unwrap();
        let count = index
            .ingest_tile_with_coordinates(
                include_bytes!("../testdata/z14.pbf").to_vec(),
                2624,
                5718,
                14,
            )
            .expect("Failed to ingest tile.");
        index.commit_ingestion().unwrap();

        dbg!(count);
        dbg!(start.elapsed());
        let results = dbg!(
            index
                .search_phrase("vital creation\\'s") // Should return Vital Creations.
                .expect("Failed to execute search")
        );
        assert_eq!(
            results.first().unwrap().tag("name").unwrap(),
            "Vital Creations Vegan Cafe"
        )
    }
}
