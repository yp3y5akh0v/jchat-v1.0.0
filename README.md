# jchat-v1.0.0
simple chat from scratch with commands

This project is written to cover as much as possible the basic topics of programming.
It includes OOP, networking, multithreading, patterns, regular expressions, and much more :). 
The project will change over time (fixing bugs, adding new features, etc.).
There're 3 main executable files: `AuthServer`, `Server` and `User`.
First 2 files contain command line arguments. `AuthServer` has `authorization server port` argument,
`Server` has `server port`, `authorization server ip`, `authorization server port` arguments. 
`User` reads arguments from `config.properties` file which includes information about both servers.
Starting sequence of files is `AuthServer -> Server -> User`. First 2 files can be run through IDE.
For `User` simple make jar file and run it through the console. Enjoy!

*If you have any questions about this project or if you find bugs :) leave comments here or send me an email to <code>yuriy.peysakhov@gmail.com</code>*
