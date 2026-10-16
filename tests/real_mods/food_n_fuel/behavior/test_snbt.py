import unittest
from snbt import parse, furnace

class SnapshotTest(unittest.TestCase):
    def test_actual_typed_nested_state(self):
        raw='[Server thread/INFO]: 0, 100, 0 has the following block data: {BurnTime:3200s,CookTime:1s,CookTimeTotal:200s,Items:[{Slot:0b,id:"minecraft:cobblestone",count:20}],RecipesUsed:{"minecraft:stone":1}}\nACK'
        state=furnace(raw)
        self.assertEqual(state['burn'],3200)
        self.assertEqual(state['slots'],{0:{'id':'minecraft:cobblestone','count':20}})
        self.assertEqual(state['recipesUsed'],{'minecraft:stone':1})
    def test_error_is_not_an_empty_furnace(self):
        with self.assertRaises(ValueError):furnace('That position is not loaded')
    def test_missing_state_is_not_defaulted(self):
        with self.assertRaises(ValueError):furnace('has the following block data: {Items:[]}')
    def test_duplicate_fields_rejected(self):
        with self.assertRaises(ValueError):parse('{BurnTime:1s,BurnTime:0s}')
    def test_incomplete_and_extra_data_rejected(self):
        for s in ['{a:[1,2}', '{a:"no end}', '{} trailing']:
            with self.assertRaises(ValueError):parse(s)
    def test_nested_components_and_typed_arrays(self):
        self.assertEqual(parse('{a:[B;1b,2b],b:{c:0.5f},d:"fnf:test"}'),{'a':[1,2],'b':{'c':.5},'d':'fnf:test'})
if __name__=='__main__':unittest.main()
