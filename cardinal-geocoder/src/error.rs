use tantivy::{TantivyError, query::QueryParserError};

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
    Io(#[from] std::io::Error),
}

impl From<mvt_reader::error::ParserError> for AirmailError {
    fn from(err: mvt_reader::error::ParserError) -> Self {
        AirmailError::Mvt(err.to_string())
    }
}
