"""Keep the global fixture identity fixed while only its Registry attachment switch changes."""
from pathlib import Path
import importlib.util
spec=importlib.util.spec_from_file_location('helper_cohort',Path(__file__).resolve().parents[1]/'helper_registry/cohort.py')
helper=importlib.util.module_from_spec(spec);spec.loader.exec_module(helper)
def validate(contract,control,treatment):
 for receipt,value in [(control,'false'),(treatment,'true')]:
  if receipt.get('fixtureEnvironment')!={'DF4C3_VARIANT':'combined','DF4C3_REGISTER':value}:raise ValueError('Global fixture condition mismatch')
 c={**control,'fixtureEnvironment':{'DF4C3_REGISTER':'false'}};t={**treatment,'fixtureEnvironment':{'DF4C3_REGISTER':'true'}}
 return helper.validate(contract,c,t,register_environment='DF4C3_REGISTER')
