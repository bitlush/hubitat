# Somfy TaHoma Switch Driver for Hubitat

This is a Hubitat driver to integrate TaHoma Switch with Hubitat.

## Setup Instructions

- Add the driver source code files to hubitat

- Enable **Developer Mode** for your TaHoma Switch in your Somfy account online

- Make a note of your TaHoma Switch's PIN

- Add a new virtual device to Hubitat using this TaHoma Switch driver

- Add your Somfy username, password and TaHoma Switch PIN to the preferences

- Choose your Region

- The driver will attempt to guess the IP address of your TaHoma Switch, if the driver isn't working, add the IP address of your TaHoma Switch to the Host setting in the driver's preferences (make sure you assign a static IP to your TaHoma Switch in your home router's settings otherwise its IP might change)

- Click Register (this will authorise Hubitat to connect to your TaHoma Switch over the internet). All subsequent calls are local.

- Click Refresh Devices

- Refresh the device page

## Limitations

This currently only works with RTS and IO devices, however, the driver does not check on the open/close status of IO devices and treats them like RTS devices. Feel free to contribute if you have other Somfy devices that need supporting, it should be easy to add them.

## Advanced Configuration

You can now ask the driver to "retry" sending commands to devices. This is useful because communication from the TaHoma switch to RTS devices isn't 100% reliable.