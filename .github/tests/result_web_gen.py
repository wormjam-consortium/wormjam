import json
from pathlib import Path
from bs4 import BeautifulSoup

from json2html import *


data = json.load(open("results.json", "r"))
meta = data["meta"]
tests = data["tests"]
meta_html = json2html.convert(json=meta)
test_keys = list(tests.keys())
settings_path = Path(".github") / "tests" / "settings.json"
settings = json.load(open(settings_path, "r"))["pipeline"]

###Templates
collapsible_template = """<div class="card">
    <div class="card-header" id="{0}">
      <h2 class="mb-0">
        <button class="btn btn-link btn-block text-left collapsed" type="button" data-toggle="collapse" data-target="#collapse{0}" aria-expanded="false" aria-controls="collapse{0}">
          {1}
        </button>
      </h2>
    </div>
    <div id="collapse{0}" class="collapse" aria-labelledby="{0}" data-parent="#results">
      <div class="card-body">
			{2}
      </div>
    </div>
  </div>"""
toc_template = """<li><a href="#{0}">{1}</a></li>"""

###building inserts
collapsible_insert = ""
toc_insert = ""
for test in test_keys:
    toc_insert += (
        toc_template.format(
            test,
            test.replace("test", "")
            .replace("_", " ")
            .title()
            .replace("Ids", "IDs")
            .replace("Id ", "ID "),
        )
        + "\n"
    )
    collapsible_insert += (
        collapsible_template.format(
            test,
            test.replace("test", "")
            .replace("_", " ")
            .title()
            .replace("Ids", "IDs")
            .replace("Id ", "ID "),
            json2html.convert(json=tests[test]),
        )
        + "\n"
    )


# double curly brackets for escaping string formatting
blob = f"""<html>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<head>
<title>Curation Report</title>
<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.1/css/bootstrap.min.css" integrity="sha384-VCmXjywReHh4PwowAiWNagnWcLhlEJLA5buUprzK8rxFgeH0kww/aWY76TfkUoSX" crossorigin="anonymous">
<script src="https://kit.fontawesome.com/7c4a4af2ca.js" crossorigin="anonymous"></script>
<style>

table {{
	max-width:100%;
}}

th, td {{
  overflow-wrap: anywhere;
  width: 25%;
}}

#toc_container {{
    background: #f9f9f9 none repeat scroll 0 0;
    border: 1px solid #aaa;
    display: table;
    font-size: 14;
    margin-bottom: 1em;
    padding: 20px;;
	width: 100%;
	overflow-x:hidden;
}}

.toc_title {{
    font-weight: 700;
    text-align: center;
	font-size: 22;
}}


#toc_container li, #toc_container ul, #toc_container ul li{{
    list-style: outside none none !important;
	columns: auto 2
}}

.back-to-top {{
    position: fixed;
    bottom: 25px;
    right: 25px;
}}

</style>


</head>
<body>
<h1>Report for the {settings["name"]} model for <i>{settings["organism"]}</i></h1>
<small class="text-muted">Generated {meta["timestamp"]} UTC+00</small>
<div id="toc_container">
<p class="toc_title">Contents</p>
<ul class="toc_list">
  <li><a href="#Metadata">Metadata</a></li>
{toc_insert}
</ul>
</div>

<div class="accordion" id="results">
  <div class="card">
    <div class="card-header" id="Metadata">
      <h2 class="mb-0">
        <button class="btn btn-link btn-block text-left" type="button" data-toggle="collapse" data-target="#collapseOne" aria-expanded="false" aria-controls="collapseOne">
          Metadata
        </button>
      </h2>
    </div>

    <div id="collapseOne" class="collapse show" aria-labelledby="Metadata" data-parent="#results">
      <div class="card-body">
		{meta_html}
      </div>
    </div>
  </div>
  {collapsible_insert}
  
</div>
<a id="back-to-top" href="#" class="btn btn-dark btn-lg back-to-top" role="button"><i class="fas fa-chevron-up"></i></a>




<script src="https://code.jquery.com/jquery-3.5.1.slim.min.js" integrity="sha384-DfXdz2htPH0lsSSs5nCTpuj/zy4C+OGpamoFVy38MVBnE+IbbVYUew+OrCXaRkfj" crossorigin="anonymous"></script>
<script src="https://cdn.jsdelivr.net/npm/popper.js@1.16.1/dist/umd/popper.min.js" integrity="sha384-9/reFTGAW83EW2RDu2S0VKaIzap3H66lZH81PoYlFhbGU+6BZp6G7niu735Sk7lN" crossorigin="anonymous"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.1/js/bootstrap.min.js" integrity="sha384-XEerZL0cuoUbHE4nZReLT7nx9gQrQreJekYhJD9WNWhH8nEW+0c5qq7aIo2Wl30J" crossorigin="anonymous"></script>
<script>
$(document).ready(function(){{
        $(window).scroll(function () {{
                        if ($(this).scrollTop() > 50) {{
                                $('#back-to-top').fadeIn();
                        }} else {{
                                $('#back-to-top').fadeOut();
                        }}
                }});
                // scroll body to 0px on click
                $('#back-to-top').click(function () {{
                        $('body,html').animate({{
                                scrollTop: 0
                        }}, 400);
                        return false;
                }});
}});
</script>
</body>
</html>"""

blob = blob.replace(
    """<table border="1">""",
    """<table class="table table-sm table-bordered table-responsive">""",
)
blob = blob.replace("""failed""", """failed <i class="fas fa-times-circle"></i>""")
blob = blob.replace("""passed""", """passed <i class="fas fa-check-circle"></i>""")

soup = BeautifulSoup(blob, features="lxml")

with open("Report.html", "w+", encoding="utf-8") as f:
    f.write(soup.prettify(formatter="html5"))
