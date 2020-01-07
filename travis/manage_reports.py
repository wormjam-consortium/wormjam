import os
import shutil
import datetime
for i in os.listdir():
    if "travis_wait" in i:
        os.remove(i)
print("Clean up complete!")