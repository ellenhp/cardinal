#![forbid(unsafe_code)]
#![warn(clippy::missing_panics_doc)]

uniffi::setup_scaffolding!();

pub mod error;
pub mod index;
pub mod languages;
pub mod poi;
pub mod substitutions;
