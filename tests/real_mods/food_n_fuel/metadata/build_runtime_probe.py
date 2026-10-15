"""Build diagnostic SPI inputs; stock runtime evidence still needs run_stock.py."""
import argparse
import importlib.util
import os
from pathlib import Path
import zipfile

REPO = Path(__file__).resolve().parents[4]
spec = importlib.util.spec_from_file_location('workflow', REPO / 'tools/lib/unix_workflow.py')
w = importlib.util.module_from_spec(spec)
spec.loader.exec_module(w)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('jar', 'server-input', 'jdk-home'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--mode', choices=('parser', 'selection'), required=True)
    args = parser.parse_args()
    for proc in Path('/proc').iterdir():
        try:
            if (proc / 'exe').resolve().name in ('java', 'javac'):
                raise RuntimeError('All JVM work must be serial')
        except (FileNotFoundError, PermissionError):
            pass
    run = w.run_directory(REPO / 'out/convergence-007c1/runtime-probe-build')
    name = {'parser': 'RuntimeParserProbe', 'selection': 'RuntimeSelectionProbe'}[args.mode]
    source = Path(__file__).with_name(name + '.java')
    libs = sorted((args.server_input / 'libraries').rglob('*.jar'))
    inputs = w.fingerprints([args.jar, source, Path(__file__), *libs])
    w.save(run / 'inputs.json', inputs)
    classes = run / 'classes'; classes.mkdir()
    binary = w.tools(args, run)
    w.native(run, 'javac', [binary / 'javac', '--release', '21', '-cp', os.pathsep.join(map(str, [args.jar, *libs])), '-d', classes, source])
    jar = run / ('runtime-' + args.mode + '-probe.jar')
    with zipfile.ZipFile(jar, 'w') as z:
        z.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\nAutomatic-Module-Name: dualforge.metadata.probe\nFMLModType: LIBRARY\n\n')
        provider = 'io.github.gfander.dualforge.metadata.' + name + '\n'
        z.writestr('META-INF/services/net.neoforged.neoforgespi.locating.IModFileReader', provider)
        if args.mode == 'selection':
            z.writestr('META-INF/services/cpw.mods.modlauncher.api.ITransformationService', provider)
        for p in classes.rglob('*.class'):
            z.write(p, p.relative_to(classes))
    w.unchanged(inputs)
    w.save(run / 'result.json', {'status': 'PASS', 'mode': args.mode, 'compatSHA256': w.digest(args.jar), 'jar': w.fingerprints([jar])[0]})
    print('RESULT PASS', run)


if __name__ == '__main__':
    main()
