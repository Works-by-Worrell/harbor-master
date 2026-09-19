rootProject.name = "harbor-master"

include(
    "modules:common-domain",
    "modules:approach-watcher",
    "modules:stevedore-extractor",
    "modules:quarantine-validator",
    "modules:signal-tower",
)
