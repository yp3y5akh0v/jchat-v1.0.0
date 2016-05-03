# jchat-v1.0.0
simple chat from scratch with commands

This project is written to cover as much as possible the basics topics of programming.
It includes OOP, networking, multithreading, patterns, regular expressions, and much more :). 
The project will change over time (fixing bugs, adding new features, etc.).
There're 3 main executable files: <code>AuthServer</code>, <code>Server</code> and <code>User</code>.
First two files contains command line arguments. AuthServer (<code>authorization server port</code>),
Server (<code>server port, authorization server ip, authorization server port</code>). 
User reads arguments from <code>config.properties</code> file which includes information about both servers.
Starting sequence of files is <code>AuthServer -> Server -> User</code>. First two files can be run through ide.
For User simple make jar and run it through the console. Enjoy!

if you've any questions about this project or if you find bugs :) put comments here or send me message on
<code>yuriy.peysakhov@gmail.com</code>
