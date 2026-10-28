# Rebuild exports

This directory contains only PNG/model exports required by the resource rebuild chain. Draft selections are defined by tools/check_resource_rebuild.py and tools/install_native_block_art.py. Current user-authored item exports live under approved/; current connected shells use drafts/array_connected_16_v02/runtime/.

Unselected local experiments, prompts, editor history, Blender previews and machine-specific drawing scripts are excluded. Older exports retained here are inputs to intermediate installation stages; the final installer selects approved art and prunes retired runtime textures. Ordinary builds use committed runtime resources and need no art tools.
