# hubitat
### Heltun Touch Panel Switch Drivers

Hubitat drivers for Heltun Touch Panel switches.

### Key features

- Exposes all button and relay endpoints
- Optimised for response speed

## Overview

Heltun Touch Panel switches are highly configurable Z-Wave based switches with exceptional build quality.

## Using the driver

- Once installed
- Click 'Clear State'
- Click 'Configure'
- Refresh the settings page
- Edit your preferences
- Save you preferences
- Click 'Configure Parameters'
- Enjoy

Preferences in Hubitat do not automatically sync with a device. This can be the case if you configure a device with a different driver or the Z-Wave tweaker. Click 'Refresh Parameters' to sync your preferences with your Heltun Touch Panel switch. You will need to refresh the browser UI after you do this.

## Optimised for speed

A lot of trial and error was done to ensure the driver reacts quickly to your Heltun Touch Panel switch. Typically two-way or three-way switches will respond in around 600ms.

Speed tips:

- Always link your automation to button presses where possible as relay events fire after button events.
- Pressing buttons on and off very fast will overwhelm your Hubitat hub and automation will grind to a halt, this is not a limitation with the Heltun Touch Panel Switch, it's a bottleneck in Hubitat's processing.

## Tricks

The built-in Hubitat driver "Device" is useful for clearing any children, scheduled jobs or state left around from other drivers.

If buttons get out of sync (e.g. in a two-way switch in the rare event of a command getting lost) simply restart your hub or turn the offending switch on and off. Z-Wave does not guarentee messages will be delivered.

## Developer notes

The Heltun Touch Panel switch is a multichannel device exposing 2x as many endpoints as buttons, one for each button and relay. A correct driver must enable multichannel association and disable single channel association.

The SwitchBinaryReport for a button is the first event fired by a button click, this driver is optimised around that behaviour.

## Builder notes

When replacing traveller wires in two-way, three-way etc setups, wire the travellers into the secondary switches, that way you will get an instant "on" from any switch. The off will be delayed by the time it takes all switches to turn off so the circuit as the circuit is only off once all relays are open. This is the best configuration.
