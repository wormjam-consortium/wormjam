import os
import csv


class ModelSystem:
    """Class for reading SBtab files"""

    def __init__(self):
        """Initialization function"""
        self.tables = {}
        self.size = (
            {}
        )  # potentially worth removing this - It logs number of entries in every table, but no longer needed.

    def _load_table(self, name, filename):
        """Function to import a SBtab file into the ModelSystem, using the SBtable class"""
        self.tables[name] = SBtable(filename)
        self.size[name] = self.tables[name].rows - 2

    def load_folder(self, name):
        """Function to bulk import multiple SBtab files using a folder and _load_table"""
        success = False
        if os.path.isdir(name) == False:
            print(
                "The curation folder cannot be found. Unable to build the model. Aborting."
            )
            exit(1)
        else:
            print("Folder loaded")
            paths = []
            for f in os.listdir(name):
                if "SBtab.tsv" in f:
                    filename = f.replace("-SBtab.tsv", "")
                    paths.append(filename)
            try:
                assert paths != [], "There were no SBtab files found in " + name
            except AssertionError as error:
                print(error)
                exit(1)
            else:
                print("SBtab files found! Loading now!")
                self.count = 1
                for sbfile in paths:
                    print(" ".join(["Loading file:", sbfile]))
                    self._load_table(sbfile, name + "/" + sbfile + "-SBtab.tsv")

                print(" ".join([str(len(paths)), "files loaded into the model"]))
                success = True

    def validate_rxn_mets(self):
        """Function to check that all metabolites included in reactions are in the compounds table"""
        met_list = self.tables.get("Compound").data.keys()
        rxn_met_list = {}
        for key, val in self.tables.get("Reaction").data.items():
            r, p = self._process_reaction_string(val["!ReactionFormula"])
            sub_mets = []
            sub_mets.extend(r.keys())
            sub_mets.extend(p.keys())
            rxn_met_list[key] = sub_mets
        missing = {
            key: [met for met in val if met not in met_list]
            for key, val in rxn_met_list.items()
            if any(met not in met_list for met in val)
            and [met for met in val if met not in met_list] != [""]
        }
        return missing

    def _process_reaction_string(self, rxn):
        """Helper function to parse reaction strings"""

        r, p = rxn.split("<=>")

        def quick(frag):
            """splitting function"""
            frag = frag.split("+")
            frag = [
                i.rstrip().lstrip() for i in frag
            ]  # remove leading and trailing whitespace.
            frag = [i.split(" ") for i in frag]  # split into each compound
            return frag

        r = quick(r)
        p = quick(p)
        # packaging
        reactants = {
            (i[1] if len(i) == 2 else i[0]): (i[0] if len(i) == 2 else "1") for i in r
        }
        products = {
            (i[1] if len(i) == 2 else i[0]): (i[0] if len(i) == 2 else "1") for i in p
        }
        for d in [reactants, products]:
            for key, val in d.items():
                try:
                    d[key] = str(float(val))
                except:
                    pass
        return (reactants, products)


class SBtable:
    """Importable class for loading SBTab files\nConverts SBTab as nested dictionary.\n

    instance.data = Dictionary of entries in SBTab\n
    Each entry is a dictionary of the data associated with that entry, with column headers as keys.

        Arguments:
            xlsx {str} -- Path to SBTab file of interest.

        Keyword Arguments:
            headerRow {int} -- Excel row of the header information, (default: {2})
            mode {str} -- version of SBtable to load
    """

    def __init__(self, filename, headerRow=2):
        """Loads the SBTab file"""
        self.name = filename
        with open(filename, encoding="latin-1") as tsvfile:
            tsv = csv.reader(tsvfile, delimiter="\t")
            entries = []
            for row in tsv:
                if tsv.line_num == 1:  # row 1 - SBtab DocString
                    self.sbString = row[0]
                elif tsv.line_num == 2:  # row 2 - headers of the table
                    self.headers = row
                else:
                    entries.append(row)
            # define size of data
            self.cols = len(self.headers)
            self.rows = len(entries) + 2
            # create the nested dict object
            try:
                self.data = {
                    entry[0]: {
                        self.headers[i]: (
                            entry[i] if len(entry) >= len(self.headers) else ""
                        )
                        for i in range(1, len(self.headers))
                    }
                    for entry in entries
                }
                while "" in self.data:
                    self.data.pop("")
            except:
                print(self.name)
                print("tsv import failed. Aborting...")
                exit()
            # remove blank entries
