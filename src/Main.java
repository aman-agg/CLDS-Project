import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws java.io.IOException{
	// write your code here
        SequentialRTree rTree = new SequentialRTree();
        Scanner sc=new Scanner(System.in);
        int n = sc.nextInt();
        System.out.println("Add points for addition : ");
        for(int i=0;i<n;++i)
        {
            int x = sc.nextInt();
            int y = sc.nextInt();
            rTree.add(new Point(x,y));
            continue;
        }
    }
}
