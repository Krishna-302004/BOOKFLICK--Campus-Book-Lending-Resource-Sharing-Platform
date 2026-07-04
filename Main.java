package com.bookshare;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            // Start all three servers in separate threads
            new Thread(() -> {
                try {
                    BookServer.main(args);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                try {
                    RegisterServer.main(args);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        //    new Thread(() -> {
          ///    try {
          //          LoginServer.main(args);
           //     } catch (IOException e) {
           //         e.printStackTrace();
          //      }
          //  }).start();

            System.out.println("All servers started!");
            //System.out.println("Book Server: http://localhost:9090/");upload
            System.out.println("Register: http://localhost:9091/register");//registration
            System.out.println("home: http://localhost:9094/books");
        } catch (Exception e) {
            e.printStackTrace();
        }


        }
    public class UserContext {
        private static int sid = -1; // Default value indicating no user

        public static void setSid(int sid) {
            UserContext.sid = sid;
        }

        public static int getSid() {
            return sid;
        }

    }
}