use std::collections::HashMap;

use geo::{Centroid, Geometry};
use serde::{Deserialize, Serialize};
use uniffi::Record;

use crate::substitutions::permute_road;

#[derive(Debug, Clone, Serialize, Deserialize, Record)]
pub struct PointOfInterest {
    lat: f64,
    lng: f64,
    tags: Vec<KvPair>,
}

impl PointOfInterest {
    pub fn new(lat: f64, lng: f64, tags: Vec<(String, String)>) -> Self {
        Self { lat, lng, tags: tags.into_iter().map(|(key, value)| KvPair{key, value}).collect() }
    }

    pub fn tags(&self) -> HashMap<String, String> {
        return self.tags.iter().map(|kv_pair| (kv_pair.key.clone(), kv_pair.value.clone())).collect();
    }

    pub fn tag(&self, key: &str) -> Option<String> {
        // This is a little verbose but no sense constructing a hashmap of all tags if we don't need to.
        self.tags
            .iter()
            .filter(|kv_pair| kv_pair.key == key)
            .map(|kv_pair| kv_pair.value.clone())
            .next()
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, Record)]
pub struct KvPair {
    pub key: String,
    pub value: String,
}

#[derive(Debug, Clone)]
pub(crate) struct InputPoi {
    pub names: Vec<String>,
    pub house_number: Option<String>,
    pub road: Option<String>,
    pub unit: Option<String>,
    pub admins: Vec<String>,
    pub s2cell: u64,
    pub tags: Vec<(String, String)>,
    pub languages: Vec<String>,
}

impl InputPoi {
    pub fn from_tags(
        lang: &str,
        geometry: Geometry<f32>,
        tags: &HashMap<String, String>,
    ) -> Option<InputPoi> {
        let house_number = tags.get("addr:housenumber").map(ToString::to_string);
        let road = tags.get("addr:street").map(ToString::to_string);
        let unit = tags.get("addr:unit").map(ToString::to_string);

        let names = {
            let names: Vec<String> = tags
                .iter()
                .filter(|(key, _value)| key.contains("name:") || *key == "name")
                .map(|(_k, v)| v)
                .cloned()
                .collect();
            names
        };

        if (house_number.is_none() || road.is_none()) && names.is_empty() {
            return None;
        }

        let tags = tags.iter().map(|(k, v)| (k.clone(), v.clone())).collect();
        let centroid = geometry.centroid()?;
        let s2cell = s2::cellid::CellID::from(s2::latlng::LatLng::from_degrees(
            centroid.y() as f64,
            centroid.x() as f64,
        ))
        .0;

        Some(InputPoi {
            names,
            house_number,
            road,
            unit,
            admins: vec![], // TODO
            s2cell,
            tags,
            languages: vec![lang.to_string()],
        })
    }
}

#[derive(Debug)]
pub(crate) struct SchemafiedPoi {
    pub content: Vec<String>,
    pub s2cell: u64,
    pub s2cell_parents: Vec<u64>,
    pub tags: Vec<(String, String)>,
}

fn prefix_strings<I: IntoIterator<Item = String>>(prefix: &str, strings: I) -> Vec<String> {
    return strings
        .into_iter()
        .map(|s| format!("{}={}", prefix, s))
        .collect();
}

impl From<InputPoi> for SchemafiedPoi {
    fn from(poi: InputPoi) -> Self {
        let mut content = Vec::new();
        content.extend(prefix_strings("", poi.names));
        content.extend(prefix_strings("", poi.house_number));
        if let Some(road) = poi.road {
            for lang in poi.languages {
                content.extend(prefix_strings("", permute_road(&road, &lang)));
            }
        }
        content.extend(prefix_strings("", poi.unit));
        content.extend(prefix_strings("", poi.admins));

        for (key, value) in &poi.tags {
            content.extend(prefix_strings(
                "",
                value.split(";").map(|v| format!("{key}={v}")),
            ));
        }

        let mut s2cell_parents = Vec::new();
        let cell = s2::cellid::CellID(poi.s2cell);
        for level in 0..cell.level() {
            let cell = cell.parent(level);
            s2cell_parents.push(cell.0);
        }

        Self {
            content,
            s2cell: poi.s2cell,
            s2cell_parents,
            tags: poi.tags,
        }
    }
}
