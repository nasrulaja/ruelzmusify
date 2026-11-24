cmake_minimum_required(VERSION 3.22.1)
project(ruelzmusify_native LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_library(log-lib log)

include(FetchContent)
FetchContent_Declare(libusb
    URL https://github.com/libusb/libusb/releases/download/v1.0.27/libusb-1.0.27.tar.bz2
    URL_HASH SHA256=ffaa41d741a8a3bee244ac8e54a72ea05bf2879663c098c82fc5757853441575)
FetchContent_MakeAvailable(libusb)

add_library(ruelzmusify_libusb STATIC
    ${libusb_SOURCE_DIR}/libusb/core.c
    ${libusb_SOURCE_DIR}/libusb/descriptor.c
    ${libusb_SOURCE_DIR}/libusb/hotplug.c
    ${libusb_SOURCE_DIR}/libusb/io.c
    ${libusb_SOURCE_DIR}/libusb/sync.c
    ${libusb_SOURCE_DIR}/libusb/strerror.c
    ${libusb_SOURCE_DIR}/libusb/os/linux_usbfs.c
    ${libusb_SOURCE_DIR}/libusb/os/events_posix.c
    ${libusb_SOURCE_DIR}/libusb/os/threads_posix.c
    ${libusb_SOURCE_DIR}/libusb/os/linux_netlink.c)
target_include_directories(ruelzmusify_libusb
    PRIVATE ${libusb_SOURCE_DIR}/android ${libusb_SOURCE_DIR}/libusb/os
    PUBLIC ${libusb_SOURCE_DIR}/libusb)
target_compile_options(ruelzmusify_libusb PRIVATE -O2 -fvisibility=hidden -pthread -w)
set_target_properties(ruelzmusify_libusb PROPERTIES POSITION_INDEPENDENT_CODE ON)

add_library(ruelzmusify_usb SHARED bitperfect/usb_transport.cpp)
target_compile_options(ruelzmusify_usb PRIVATE -O2 -Wall -Wextra -fvisibility=hidden)
target_link_libraries(ruelzmusify_usb PRIVATE ruelzmusify_libusb ${log-lib})
target_link_options(ruelzmusify_usb PRIVATE
    "-Wl,-z,max-page-size=16384"
    "-Wl,--gc-sections"
    "-Wl,--version-script,${CMAKE_CURRENT_SOURCE_DIR}/bitperfect/exports.map")
set_property(TARGET ruelzmusify_usb APPEND PROPERTY LINK_DEPENDS
    "${CMAKE_CURRENT_SOURCE_DIR}/bitperfect/exports.map")
