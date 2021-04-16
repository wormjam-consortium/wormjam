import datetime
import subprocess
from pathlib import Path
from support.helper_classes import ModelConfig

settings_path = Path(".github") / "tests" / "config.yml"
settings = ModelConfig(settings_path)

timestamp = datetime.datetime.now().isoformat(timespec='minutes') 
subprocess.call(["tar","czvf",f'{settings.name}-{timestamp}.tar.gz',f'{settings.name}.xml',"--force-local"])
