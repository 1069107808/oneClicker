# oneClicker
This clicker relies on Accessibility Services and Overlay permission.

It will create a floating menu and window on the screen, any touch events
outside the floating window will not be affected, users can touch on the floating window to move or resize it to an appropriate place before clicking.

After clicking on the "start" button on the menu, the floating window will not be able to move or resize until users click on "pause" on the menu.
During this period, the touch events outside the window still not be affected, but once user touch on the floating window it will obtain it as the location
and keep launching gesture to the screen through Accessibility Services, and now any touch events outside the floating window will be considered
as "stop clicking", but user still can touch on the floating window to change the click location.
