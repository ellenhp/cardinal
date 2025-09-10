use std::collections::HashMap;

use geo::{Centroid, Coord, Geometry, Point};
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
        Self {
            lat,
            lng,
            tags: tags
                .into_iter()
                .map(|(key, value)| KvPair { key, value })
                .collect(),
        }
    }

    pub fn tags(&self) -> HashMap<String, String> {
        return self
            .tags
            .iter()
            .map(|kv_pair| (kv_pair.key.clone(), kv_pair.value.clone()))
            .collect();
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
    pub fn from_tags_with_transform(
        lang: &str,
        geometry: Geometry<f32>,
        tags: &HashMap<String, String>,
        tile_x: u32,
        tile_y: u32,
        tile_z: u8,
        extent: u32,
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

        // Transform geometry from tile coordinates to lat/lng
        let transformed_geometry =
            transform_tile_geometry(geometry, tile_x, tile_y, tile_z, extent)?;
        let centroid = transformed_geometry.centroid()?;
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

fn transform_tile_geometry(
    geometry: Geometry<f32>,
    tile_x: u32,
    tile_y: u32,
    tile_z: u8,
    extent: u32,
) -> Option<Geometry<f32>> {
    use geo::{Coord, LineString, Polygon};

    match geometry {
        Geometry::Point(mut point) => {
            let transformed = transform_coord(point.0, tile_x, tile_y, tile_z, extent);
            point.0 = transformed;
            Some(Geometry::Point(point))
        }
        Geometry::LineString(mut line_string) => {
            let transformed_coords: Vec<Coord<f32>> = line_string
                .0
                .into_iter()
                .map(|coord| transform_coord(coord, tile_x, tile_y, tile_z, extent))
                .collect();
            line_string.0 = transformed_coords;
            Some(Geometry::LineString(line_string))
        }
        Geometry::Polygon(polygon) => {
            let transformed_exterior: Vec<Coord<f32>> = polygon
                .exterior()
                .0
                .iter()
                .map(|coord| transform_coord(*coord, tile_x, tile_y, tile_z, extent))
                .collect();

            let transformed_interiors: Vec<LineString<f32>> = polygon
                .interiors()
                .iter()
                .map(|interior| {
                    let coords: Vec<Coord<f32>> = interior
                        .0
                        .iter()
                        .map(|coord| transform_coord(*coord, tile_x, tile_y, tile_z, extent))
                        .collect();
                    LineString(coords)
                })
                .collect();

            Some(Geometry::Polygon(Polygon::new(
                LineString(transformed_exterior),
                transformed_interiors,
            )))
        }
        Geometry::MultiPoint(mut multi_point) => {
            let transformed_coords: Vec<Point<f32>> = multi_point
                .0
                .into_iter()
                .map(|coord| Point(transform_coord(coord.0, tile_x, tile_y, tile_z, extent)))
                .collect();
            multi_point.0 = transformed_coords;
            Some(Geometry::MultiPoint(multi_point))
        }
        Geometry::Line(mut line) => {
            let start = transform_coord(line.start, tile_x, tile_y, tile_z, extent);
            let end = transform_coord(line.end, tile_x, tile_y, tile_z, extent);
            line.start = start;
            line.end = end;
            Some(Geometry::Line(line))
        }
        Geometry::MultiLineString(mut multi_line_string) => {
            let transformed_lines: Vec<LineString<f32>> = multi_line_string
                .0
                .into_iter()
                .map(|mut line_string| {
                    let transformed_coords: Vec<Coord<f32>> = line_string
                        .0
                        .into_iter()
                        .map(|coord| transform_coord(coord, tile_x, tile_y, tile_z, extent))
                        .collect();
                    line_string.0 = transformed_coords;
                    line_string
                })
                .collect();
            multi_line_string.0 = transformed_lines;
            Some(Geometry::MultiLineString(multi_line_string))
        }
        Geometry::MultiPolygon(mut multi_polygon) => {
            let transformed_polygons: Vec<Polygon<f32>> = multi_polygon
                .0
                .into_iter()
                .map(|polygon| {
                    let transformed_exterior: Vec<Coord<f32>> = polygon
                        .exterior()
                        .0
                        .iter()
                        .map(|coord| transform_coord(*coord, tile_x, tile_y, tile_z, extent))
                        .collect();

                    let transformed_interiors: Vec<LineString<f32>> = polygon
                        .interiors()
                        .iter()
                        .map(|interior| {
                            let coords: Vec<Coord<f32>> = interior
                                .0
                                .iter()
                                .map(|coord| {
                                    transform_coord(*coord, tile_x, tile_y, tile_z, extent)
                                })
                                .collect();
                            LineString(coords)
                        })
                        .collect();

                    Polygon::new(LineString(transformed_exterior), transformed_interiors)
                })
                .collect();
            multi_polygon.0 = transformed_polygons;
            Some(Geometry::MultiPolygon(multi_polygon))
        }
        Geometry::GeometryCollection(mut geometry_collection) => {
            let transformed_geometries: Vec<Geometry<f32>> = geometry_collection
                .0
                .into_iter()
                .filter_map(|geometry| {
                    transform_tile_geometry(geometry, tile_x, tile_y, tile_z, extent)
                })
                .collect();
            geometry_collection.0 = transformed_geometries;
            Some(Geometry::GeometryCollection(geometry_collection))
        }
        Geometry::Rect(mut rect) => {
            // Transform the rectangle by transforming its corners
            let min = transform_coord(rect.min(), tile_x, tile_y, tile_z, extent);
            let max = transform_coord(rect.max(), tile_x, tile_y, tile_z, extent);
            rect = geo::Rect::new(min, max);
            Some(Geometry::Rect(rect))
        }
        Geometry::Triangle(mut triangle) => {
            let mut coords = triangle.to_array();
            for coord in coords.iter_mut() {
                *coord = transform_coord(*coord, tile_x, tile_y, tile_z, extent);
            }
            triangle = geo::Triangle::from(coords);
            Some(Geometry::Triangle(triangle))
        }
    }
}

fn transform_coord(
    coord: Coord<f32>,
    tile_x: u32,
    tile_y: u32,
    tile_z: u8,
    extent: u32,
) -> Coord<f32> {
    // Normalize coordinate within tile to [0, 1] range
    let normalized_x = coord.x as f64 / extent as f64;
    let normalized_y = coord.y as f64 / extent as f64;

    // Calculate the absolute position in tile units
    let absolute_x = tile_x as f64 + normalized_x;
    let absolute_y = tile_y as f64 + normalized_y;

    // Normalize to [0, 1] range for the entire world
    let zoom_factor = (1u32 << tile_z) as f64;
    let x = absolute_x / zoom_factor;
    let y = absolute_y / zoom_factor;

    // Convert to lat/lng using Web Mercator projection
    let lng = x * 360.0 - 180.0;
    // Web Mercator latitude calculation
    // y is in [0, 1] range where 0 = top (90deg) and 1 = bottom (-90deg)
    let lat = (std::f64::consts::PI * (1.0 - 2.0 * y))
        .sinh()
        .atan()
        .to_degrees();

    Coord {
        x: lng as f32,
        y: lat as f32,
    }
}
