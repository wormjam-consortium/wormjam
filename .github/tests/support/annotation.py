from lxml import etree

#debugging
from xml.etree import ElementTree

#if no database SBtab, then db_dict is an empty dict

def _annotate(db_dict, ref):
    """Function to access reference links, and handle when those links are not in DB table
	
	Takes a dict of databases and a database name"""
    if ref in db_dict:
        return db_dict[ref]["!IdentifiersOrgPrefix"]
    else:
        return "https://identifiers.org/" + ref


def _check_db_type(db_dict, ref):
    """Function to access reference links, and handle when those links are not in DB table"""
    if ref in db_dict:
        return db_dict[ref]["!IsOrIn"]
    else:
        return "Is"


def gen_annotation_tree(metaid, db_dict, data, NS_MAP):

    annotation_tree = etree.SubElement(
    etree.SubElement(
        etree.Element("annotation"), "{%s}" % NS_MAP["rdf"] + "RDF"
    ),
    "{%s}" % NS_MAP["rdf"] + "Description",
    attrib={"{%s}" % NS_MAP["rdf"] + "about": "#" + metaid},
    )
    # get a list of which DBs are annotated for this entry, as well as what type of DB they are
    annotated_dbs = [
        db.split(":")[1]
        for db in data.keys()
        if "!Identifiers" in db and data[db] != ""
    ]

    db_types = [_check_db_type(db_dict, db) for db in annotated_dbs]

    # create bqbiol:type -> rdf:bag -> rdf:li elements
    bqbiol_is_and_rdf_bag = False
    bqbiol_occurs_in_and_rdf_bag = False
    if "Is" in db_types:
        bqbiol_is_and_rdf_bag = etree.SubElement(
            etree.SubElement(annotation_tree, "{%s}" % NS_MAP["bqbiol"] + "is"),
            "{%s}" % NS_MAP["rdf"] + "Bag",
        )
    if "In" in db_types:
        bqbiol_occurs_in_and_rdf_bag = etree.SubElement(
            etree.SubElement(annotation_tree, "{%s}" % NS_MAP["bqbiol"] + "isPartOf"),
            "{%s}" % NS_MAP["rdf"] + "Bag",
        )
    for db in annotated_dbs:
        # if bqbiol_is_and_rdf_bag or bqbiol_occurs_in_and_rdf_bag:
        # annotate to the correct bag
        if _check_db_type(db_dict, db) == "Is":
            for identifier in data["!Identifiers:" + db].split("|"):
                etree.SubElement(
                    bqbiol_is_and_rdf_bag,
                    "{%s}" % NS_MAP["rdf"] + "li",
                    attrib={
                        "{%s}" % NS_MAP["rdf"]
                        + "resource": _annotate(db_dict, db)
                        + ":"
                        + identifier
                    },
                )
        else:
            for identifier in data["!Identifiers:" + db].split("|"):
                etree.SubElement(
                    bqbiol_occurs_in_and_rdf_bag,
                    "{%s}" % NS_MAP["rdf"] + "li",
                    attrib={
                        "{%s}" % NS_MAP["rdf"]
                        + "resource": _annotate(db_dict, db)
                        + ":"
                        + identifier
                    },
                )

    return annotation_tree