import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public static int printOperations(){
        Scanner sc = new Scanner(System.in);

        System.out.println("R Tree operations");
        System.out.println("1. Add (x y)");
        System.out.println("2. Delete (x y)");
        System.out.println("3. Print Tree");
        System.out.println("4. Contains");
        System.out.println("5. Exit");
        int n = 0;
        while(n > 5 || n < 1){
            System.out.print("Choose operation number: ");
            n = sc.nextInt();
            System.out.println();
        }
        return n;
    }
    public static void runSequentialRTree(){
        SequentialRTree rTree = new SequentialRTree();
        Scanner sc = new Scanner(System.in);
        while(true){
            int operation = printOperations();
            if(operation == 1){
                System.out.print("Point for Addition: ");
                int x = sc.nextInt();
                int y = sc.nextInt();
                System.out.println();
                rTree.add(new Point(x,y));
            }
            else if(operation == 2){
                System.out.print("Points for Deletion: ");
                int x = sc.nextInt();
                int y = sc.nextInt();
                System.out.println();
                rTree.delete(new Point(x,y));
            }
            else if(operation == 3){
                rTree.scan();
            }
            else if(operation == 4){
                System.out.print("Point to check: ");
                int x = sc.nextInt();
                int y = sc.nextInt();
                System.out.println();
                System.out.println(rTree.contains(new Point(x,y)));
            }
            else if(operation == 5){
                break;
            }
            else{
                System.out.println("Incorrect input");
            }
        }
    }
    public static void runLockBasedRTree(){
        ReentrantLock sharedLock = new ReentrantLock();
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for(int i = 0;i<5;i++){
            executorService.submit(new LockBasedRTree(sharedLock));
        }

        executorService.shutdown();
    }
    public static void main(String[] args) throws java.io.IOException{
	// write your code here
        runLockBasedRTree();
//        TASlock sharedLock = new TASlock();
//        ExecutorService executorService = Executors.newFixedThreadPool(5);
//        for (int i = 0; i < 5; i++) {
//            executorService.submit(new TASLockCounter(sharedLock));
//        }
//        executorService.shutdown();
    }
}
