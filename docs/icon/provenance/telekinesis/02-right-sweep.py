from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from render_variants import main
main('telekinesis','02-right-sweep')
