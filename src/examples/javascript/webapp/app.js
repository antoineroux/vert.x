/*
This verticle contains the configuration for our application and co-ordinates
start-up of the verticles that make up the application.
 */

load('vertx.js');

var log = vertx.logger;

// Our application config

var persistorConf =  {
  address: 'demo.persistor',
  db_name: 'test_db'
};
var authMgrConf = {
  address: 'demo.authMgr',
  user_collection: 'users',
  persistor_address: 'demo.persistor'
};
var mailerConf = {
  address: 'demo.mailer'
  /*
  Uncomment this to use a gmail account
  ,
  host: 'smtp.googlemail.com',
  port: 465,
  ssl: true,
  auth: true,
  username: 'your_username',
  password: 'your_password'
  */
};


// Deploy the busmods

vertx.deployWorkerVerticle('busmods/mongo_persistor.js', persistorConf, 1, function() {
  load('static_data.js');
});
vertx.deployVerticle('busmods/auth_mgr.js', authMgrConf);
vertx.deployWorkerVerticle('busmods/mailer.js', mailerConf);


// Start the order manager

vertx.deployVerticle('order_mgr.js');

// Start the web server

vertx.deployVerticle('web_server.js');