cmake_minimum_required(VERSION 3.22.1)
project("asconjni")

add_library(asconjni SHARED
        ascon_jni.c
        ascon/aead.c
        ascon/printstate.c
)

target_include_directories(asconjni PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/ascon
)

find_library(log-lib log)
target_link_libraries(asconjni ${log-lib})