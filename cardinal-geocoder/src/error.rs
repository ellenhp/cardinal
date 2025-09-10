use tantivy::{TantivyError, query::QueryParserError};
use std::io;

#[derive(Debug, thiserror::Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum AirmailError {
    #[error("Tantivy error: {0}")]
    Tantivy(#[from] TantivyError),
    
    #[error("Query parser error: {0}")]
    QueryParser(#[from] QueryParserError),
    
    #[error("MVT reader parser error: {0}")]
    Mvt(String),
    
    #[error("Regex error: {0}")]
    Regex(#[from] regex::Error),
    
    #[error("IO error: {0}")]
    Io(#[from] io::Error),

    #[error("Please call begin_ingestion to initialize the writer transaction prior to ingesting tiles")]
    InvalidIngestionState,
}

impl From<mvt_reader::error::ParserError> for AirmailError {
    fn from(err: mvt_reader::error::ParserError) -> Self {
        AirmailError::Mvt(err.to_string())
    }
}

impl From<tantivy::directory::error::OpenDirectoryError> for AirmailError {
    fn from(err: tantivy::directory::error::OpenDirectoryError) -> Self {
        AirmailError::Io(io::Error::new(io::ErrorKind::Other, err.to_string()))
    }
}
