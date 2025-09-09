use lazy_static::lazy_static;

use crate::substitutions::SubstitutionDict;

macro_rules! language_dicts {
    ($($lang:ident),*) => {
        lazy_static! {
            $(
                static ref $lang: SubstitutionDict =
                    SubstitutionDict::from_str(include_str!(concat!("../dictionaries/", stringify!($lang), "/street_types.txt"))).unwrap();
            )*
        }
    };
}

lazy_static! {
    static ref EMPTY_SUBS: SubstitutionDict = SubstitutionDict::empty();
}

language_dicts!(
    en, si, zh, it, az, is, th, hu, lv, ca, ur, es, pap, ja, tr, gsw, mt, hi, el, sr, af, de, sv,
    hr, gl, pt, id, oc, ko, ms, lb, ar, cs, fa, eu, fi, bg, he, sl, da, ga, ka, nl, sk, cy, fr, ro,
    pl, gd, nb, lt, vi, et, bs, uk, be, br, ast, fil, ru
);

pub(crate) fn substitution_dict(iso_639_code: &str) -> &'static SubstitutionDict {
    match iso_639_code {
        "en" => &*en,
        "si" => &*si,
        "zh" => &*zh,
        "it" => &*it,
        "az" => &*az,
        "is" => &*is,
        "th" => &*th,
        "hu" => &*hu,
        "lv" => &*lv,
        "ca" => &*ca,
        "ur" => &*ur,
        "es" => &*es,
        "pap" => &*pap,
        "ja" => &*ja,
        "tr" => &*tr,
        "gsw" => &*gsw,
        "mt" => &*mt,
        "hi" => &*hi,
        "el" => &*el,
        "sr" => &*sr,
        "af" => &*af,
        "de" => &*de,
        "sv" => &*sv,
        "hr" => &*hr,
        "gl" => &*gl,
        "pt" => &*pt,
        "id" => &*id,
        "oc" => &*oc,
        "ko" => &*ko,
        "ms" => &*ms,
        "lb" => &*lb,
        "ar" => &*ar,
        "cs" => &*cs,
        "fa" => &*fa,
        "eu" => &*eu,
        "fi" => &*fi,
        "bg" => &*bg,
        "he" => &*he,
        "sl" => &*sl,
        "da" => &*da,
        "ga" => &*ga,
        "ka" => &*ka,
        "nl" => &*nl,
        "sk" => &*sk,
        "cy" => &*cy,
        "fr" => &*fr,
        "ro" => &*ro,
        "pl" => &*pl,
        "gd" => &*gd,
        "nb" => &*nb,
        "lt" => &*lt,
        "vi" => &*vi,
        "et" => &*et,
        "bs" => &*bs,
        "uk" => &*uk,
        "be" => &*be,
        "br" => &*br,
        "ast" => &*ast,
        "fil" => &*fil,
        "ru" => &*ru,
        _ => &*EMPTY_SUBS,
    }
}
