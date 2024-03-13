import java.io.*;
/**
 * Purpose: demonstrate simple Java Fraction class with command line,
 * jdb debugging, and Ant build file.
 *
 * Ser321 Foundations of Distributed Applications
 * see http://pooh.poly.asu.edu/Ser321
 * @author Tim Lindquist Tim.Lindquist@asu.edu
 *         Software Engineering, CIDSE, IAFSE, ASU Poly
 * @version January 2020
 */
public class Fraction {

   private int numerator, denominator;

   public Fraction(){
      numerator = denominator = 0;
   }

   public void print() {
    System.out.print(numerator + "/" + denominator );
   }

   public void setNumerator (int n ){
      numerator = n;
   }

   public void setDenominator (int d) {
      denominator = d;
   }

   public int getDenominator() {
      return denominator;
   }

   public int getNumerator() {
      return numerator;
   }

   public static void main (String args[]) {
      if (args.length == 2 ) {
         int argY = Integer.parseInt(args[0]);
         int argX = Integer.parseInt(args[1]);
         try {
            Fraction frac = new Fraction();
            frac.setDenominator(argY);
            frac.setNumerator(argX);
         } catch (Exception e) {
            System.out.println("Arguments: " + args[0] + ", " + args[1] + " must be integers.");
            System.exit(1);
         }
         System.out.println("The fraction is: " + argX + "/" + argY);
         System.out.println(argX + " divided by " + argY + " = " + Math.floor(argX / argY));
      }

      // This block runs in gradle task runFraction is called
      else {
         try {
            // create a new instance
            // Fraction *frac = [[Fraction alloc] init];
            Fraction frac = new Fraction();

            // set the values
            frac.setNumerator(1);
            frac.setDenominator(3);

            // print it
            System.out.print("The fraction is: ");
            frac.print();
            System.out.println("");
         }catch(Exception e) {
            e.printStackTrace();
         }
      }
   }
}

