package(default_visibility = ["//transpiler:__subpackages__"])

licenses(["notice"])  # Apache 2.0

java_library(
    name = "generator",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:gson",
        "//third_party:guava",
        "//third_party:jscomp",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/libraryinfo",
    ],
)
