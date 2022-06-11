# Somfy TaHoma Switch Driver for Hubitat

This is a Hubitat driver to integrate TaHoma Switch with Hubitat.

## Setup Instructions

- Add the driver source code files to hubitat

- Enable **Developer Mode** for your TaHoma Switch in your Somfy account online

- Make a note of your TaHoma Switch's PIN

- Add a new virtual device to Hubitat using this TaHoma Switch driver

- Add your Somfy username, password and TaHoma Switch PIN to the preferences

- Click Register (this will authorise Hubitat to connect to your TaHoma Switch over the internet). All subsequent calls are local.

- Click Refresh Devices

- Refresh the device page

## Limitations

This currently only works with RTS blinds as that's all I need. Feel free to contribute if you have other Somfy devices that need supporting, it should be easy to add them.

