import subprocess
from pathlib import Path
from support.helper_classes import ModelConfig

settings_path = Path(".github") / "tests" / "config.yml"
settings = ModelConfig(settings_path)

subprocess.call(["tar","czvf",f'{settings.name}.tar.gz',f'{settings.name}.xml'])