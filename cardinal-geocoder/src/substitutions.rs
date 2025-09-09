use std::{collections::HashMap, error::Error};

use lazy_static::lazy_static;
use regex::Regex;

use crate::languages::substitution_dict;

lazy_static! {
    static ref ASCII_WHITESPACE_RE: Regex = Regex::new(r"[ \t\r\n]+").unwrap();
}

lazy_static! {
    static ref EN_STREET_TYPES: SubstitutionDict =
        SubstitutionDict::from_str(include_str!("../dictionaries/en/street_types.txt")).unwrap();
    static ref EMPTY_SUBS: SubstitutionDict = SubstitutionDict::empty();
}

#[derive(Debug, Hash, PartialEq, Eq, Clone)]
pub(super) struct SubstitutionDict {
    subs: Vec<(String, Vec<String>)>,
}

impl SubstitutionDict {
    pub fn empty() -> Self {
        Self { subs: vec![] }
    }

    pub(super) fn from_str(contents: &str) -> Result<Self, Box<dyn Error>> {
        let mut subs: HashMap<String, Vec<String>> = HashMap::new();
        for line in contents.lines() {
            let components: Vec<_> = line.split('|').collect();
            for component in &components {
                if let Some(existing_subs) = subs.get_mut(*component) {
                    for component_to_add in &components {
                        if !existing_subs.contains(&component_to_add.to_string()) {
                            existing_subs.push(component_to_add.to_string());
                        }
                    }
                } else {
                    subs.insert(
                        component.to_string(),
                        components.iter().map(|s| s.to_string()).collect(),
                    );
                }
            }
        }
        Ok(Self {
            subs: subs.into_iter().collect(),
        })
    }

    pub fn substitute(&self, token: &str) -> Vec<String> {
        let mut substitutions = vec![token.to_string()];
        for (key, subs) in &self.subs {
            if key == token {
                substitutions.extend(subs.clone());
            }
        }
        substitutions
    }
}

fn sanitize(field: &str) -> String {
    ASCII_WHITESPACE_RE
        .replace_all(&deunicode::deunicode(field).to_lowercase(), " ")
        .to_string()
}

pub(super) fn apply_subs(
    prefix: &[String],
    remaining: &[String],
    dict: &SubstitutionDict,
) -> Vec<String> {
    if remaining.is_empty() {
        return vec![prefix.join(" ")];
    }

    let mut permutations = vec![];

    for sub in dict.substitute(&remaining[0]) {
        let mut prefix = prefix.to_vec();
        prefix.push(sub);
        let mut remaining = remaining.to_vec();
        remaining.remove(0);
        permutations.extend(apply_subs(&prefix, &remaining, dict));
    }

    permutations
}

pub fn permute_road(road: &str, lang_code: &str) -> Vec<String> {
    let sub_dict = substitution_dict(&lang_code);
    let road_tokens: Vec<String> = sanitize(road)
        .split_ascii_whitespace()
        .map(|s| s.to_string())
        .collect();
    apply_subs(&[], &road_tokens, sub_dict)
}

#[cfg(test)]
mod test {
    use crate::substitutions::permute_road;

    #[test]
    fn test_permute_road() {
        let road = "fremont avenue north";
        let permutations = permute_road(road, "en");
        dbg!(permutations.clone());
        assert_eq!(permutations.len(), 27);
    }

    #[test]
    fn test_permute_road_cat() {
        let road = "carrer de villarroel";
        let permutations = permute_road(road, "ca");
        dbg!(permutations.clone());
        assert_eq!(permutations.len(), 4);
    }
}
