# jchat-v1.0.0
simple chat from scratch with commands

This project is written to cover as much as possible the basic topics of programming.
It includes OOP, networking, multithreading, patterns, regular expressions, and much more :). 
The project will change over time (fixing bugs, adding new features, etc.).
There're 3 main executable files: <code>AuthServer</code>, <code>Server</code> and <code>User</code>.
First 2 files contain command line arguments. <code>AuthServer</code> has <code>authorization server port</code> argument,
<code>Server</code> has <code>server port, authorization server ip, authorization server port</code> arguments. 
<code>User</code> reads arguments from <code>config.properties</code> file which includes information about both servers.
Starting sequence of files is <code>AuthServer -> Server -> User</code>. First 2 files can be run through IDE.
For <code>User</code> simple make jar file and run it through the console. Enjoy!

if you have any questions about this project or if you find bugs :) put comments here or send me an email to <code>yuriy.peysakhov@gmail.com</code>
