"""Shared identifiers for the current complete-face block resources."""
FACE_ORDER = ('south', 'north', 'east', 'west', 'down', 'up')
FUNCTIONAL = ('network_planter', 'seed_storage', 'me_network_access',
              'planter_input_port', 'planter_output_port', 'essence_vault')
MODULES = ('planter_speed_module', 'planter_parallel_module', 'planter_conversion_module')
PARTS = (*FUNCTIONAL, 'planter_frame', 'cultivation_chamber', *MODULES)
NS = 'appliedagristics:block/'


def install(assets=None):
    from install_standalone_faces import install as install_faces
    install_faces(assets)


if __name__ == '__main__':
    install()
