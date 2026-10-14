# 服务端输入迁移

当前默认路径 `runtime/neoforge-server/`。只跟踪 config 下的 fml.toml、neoforge-common.toml、neoforge-server.toml；其SHA与旧位置完全相同。
完整 NeoForge21.1.256 / MC1.21.1 安装、库、defaultconfigs 和 spike000world 是本地输入，不随 Git 移动。`--server-input` 可继续指定外部安装。

用户机器若仍有 `spike000/neoforge-server/`，先停止所有JVM并保存目录清单及SHA，再从仓库根运行以下复制。保留旧数据；已有目标不同字节时立即停止，不覆盖。先提交或单独保护已有未提交变化。不要把这些本地文件加入Git。

```python
# python3 -B；从仓库根执行。可将source改为真实外部安装路径。
from pathlib import Path
import hashlib, shutil
source = Path('spike000/neoforge-server')
target = Path('runtime/neoforge-server')
assert source.is_dir() and not source.is_symlink()
files = sorted(source.rglob('*'))
assert not any(p.is_symlink() for p in files)
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
records = {p: sha(p) for p in files if p.is_file()}
for p, digest in records.items():
    dst = target / p.relative_to(source)
    assert not dst.exists() or (dst.is_file() and sha(dst) == digest), str(dst)
for p in files:
    dst = target / p.relative_to(source)
    if p.is_dir(): dst.mkdir(parents=True, exist_ok=True)
    elif p.is_file() and not dst.exists():
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(p, dst)
for p, digest in records.items():
    assert sha(p) == digest == sha(target / p.relative_to(source))
print('Copied and verified; original input retained')
```

空defaultconfigs目录也保留；不改变世界目录名 `spike000world` 或 Minecraft `level-name`。脚本不删除原始数据。用户机器实际迁移需用户执行核验，不继承云端复制PASS。

云端本轮把外部 `/workspace/dualforge-cloud/server-base` 的131个文件物理复制到新默认runtime，完整目录和SHA一致，原安装保留。复制结果保存在本轮 out/reorganization，不作为其他机器的完成证明。

```zsh
zsh tools/entry/dualforge.zsh --mode build
# 显式路径仍有效：
zsh tools/entry/dualforge.zsh --mode build --server-input runtime/neoforge-server
# smoke需要原冻结Probe和实际本轮新JAR：
zsh tools/entry/dualforge.zsh --mode smoke --server-input runtime/neoforge-server \
  --probe /实际原输入/forgeprobe.jar --jar /实际新构建/dualforge.jar --allow-server-start
```

JVM严格串行；代理/CA按实际平台配置，云端使用既有受验证的 java-network.args 和 public-key-relay。当前工具版本及实际命令以本轮结果为准。
