I have attached you woth Spec Kits guide - that will help you understand how to build the specs

Let me Tell you about the product idea

An app that runs in phone - call it Limitless sight

Properties - Light weight, highly efficient and fast 

What does this app do?
This app will use the camera of the phone in which its installed. 
Basically this is going to be a really life navigation system 

So Imagine a phone which is pointed to the road (basically the rider/driver of the bike/car will wear it (the ultimate idea is to make it a wearable glass which would have the camera's, speaker and the display's build in))

The core idea is that the camera will read the situation and identify the road, there must be a navigation system associated with this, (can we use the google-map which will be already present in the phone connected to a google account (I dont know, you decide the best option)), So now what must happen is the roads are gona be captured in a wide lens (in ethier case let the user decide which lens does he wants to use)

so the app will do the processing

Now we need to do this for the people of india - specifically the Bangalore

the main problem statement we are trying to solve here - It is dangerous, to drive/ride with the google map and the navigation voice along, the whole idea is to use the technique which the Need for speed most wanted (Game) used while the player is in a race, the direction is augemented on to the rides either side with arrow, So here in our case we will detect the road and have a similar arrow pattern perfectly on the road just like the augementation done on the road by Forza Horizon 6 while racing, the pattern will cover the entire road, (Further development can also include pedistians, pets crossing, warning of the pot holes and many more..) the pattern should not effect the visibility of the driver/biker, hence the pattern will be very beautifully navigate the biker/rider to the destination. 

We can also show the current speed of the vehicle

what are the perks?
There is no more loss of attention from the road, completely eyes on road which means lesser accidents 

Ease to ride through the roads - there are situations where there can be multiple roads moving straight (say left road the straight one is a bridge and the right road), at many a times the left and right roads along with the straight road will be moving in the same direction and at one point they all divert and the biker/driver gets confused which one to follow, because our system has get the patterns on the road, it becomes easy for him to identify the right road and easly navigate

For now the simplest proto-type I want you to accomplish and build is a mobile app that runs, asks the user which camera of the device to use (1,2,3,4 etc... (This question is really important - once the rider/driver selects the camera I want the app to do a calibration test - most wide angle camera have very bad image quality - not sure if we can use in such case you can try the other camera and select the best one that suits our scenario)), the camera is supposed to be held horizontally. once these are clear, then the app begins (ask for the destination, the source is anyway from the current point), Get the best route to the destination, the route and the camera must be in sync - Now the output the app must now use the camera and display in the screen, as in sync with navigation (map) - it must start augementing the arrows on the road (Few roads are single laned can augement on the entire road (make sure the diretion is augement on the road and doesnt get to the footpath or anyother), when you detect multiple road ie., Left side the road the driver/biker uses to go forward and the right side will be for the opposite vechile to pass in such situation only augement direction on left side, make sure the rules are followed clearly while augementing the direction on the road (For example: need to go left, if you are on the center lane, slowly before reaching the left turn you prompt the direction to move to the left lane at a safe distance and eventually when you are at the left turn it is safer to turn left), u-turns etc.. (If our system can detect the signal lights and change the colour of our augemented direction on the road even better - example green light -the augement in green colour, red light - the augemnetation on red colour, yellow light - using yellow colour (most real time signals will have multiple green signals one for left, straight, right - make sure the green light is correctly identified and then only change the colour of the direction augemented on the road) (implement based on the complexity))), the turns must be augemented clearly, it should not be sharp there must be curves in the direction augemented, having a general direction pointer on the top (this is a different pointer from the on road direction augementaion - this pointer on the top will have arrows shown and the arrow must rotate according to the direction of the route and destination) - all of the augemented navigations ideally must be shown on the wearable glass - for our poc I want you to assume that the phone display is the Glass (hence I must be able to see the direction augemented on the road, and the direction pointer on the top)

Additonally if we can include a Minimap just like the game would be great (Again suject to the complexity)

With this product description - I want you to built specs for the products which must be in great detail 

Now generate the specs on to the specs folder