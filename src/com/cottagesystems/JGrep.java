
package com.cottagesystems;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * echo "hello jeremy" | jgrep "hello (.*)" --format=\$1
 * @author jbf
 */
public class JGrep {
    
    private static void printusage() {
        System.err.println("jgrep <regex> <format> [file]");
        System.err.println("  <regex> regular expression like '.*\\-(.*).eml' ");
        System.err.println("  <format> format where matched groups are inserted.  $0 is default, or like $1 or $1[1:-4]");
        System.err.println("  [file] stdin is the default, or a file.");
        System.err.println("format");
        System.err.println("  $0 is replaced by the entire match.");
        System.err.println("  $1 is replaced by the first group.");
        System.err.println("  ${MATCHNUM} is replaced by the number of matches in the file.");
        System.err.println("  ${LINENUM} is replaced by the line number within the file.");
        System.err.println("  ${NEWLINE} is replaced by a new line.");
        System.err.println("examples:");
        System.err.println("  echo \"hello jeremy\" | jgrep \"hello (.*)\" --format=\\$1");
        System.err.println("");
        System.err.println("https://github.com/jbfaden/Utilities/blob/main/src/com/cottagesystems/JGrep.java");
        
    }
    
    private static String doSubString( String text, String spec ) {
        Pattern p= Pattern.compile("(-?\\d*)\\:(-?\\d*)");
        Matcher m= p.matcher(spec);
        if ( !m.matches() ) {
            throw new IllegalArgumentException("subString spec must be like $1[4:5]");
        } else {
            int start,stop;
            if ( m.group(1).length()>0 ) {
                start= Integer.parseInt(m.group(1));
            } else {
                start= 0;
            }
            if ( m.group(2).length()>0 ) {
                stop= Integer.parseInt(m.group(2));
            } else {
                stop= text.length();
            }
            if ( start<0 ) start= text.length() + start;
            if ( stop<0 ) stop= text.length() + stop;
            
            return text.substring(start,stop);
        }
    }
    
    public static void main( String[] args ) throws IOException {
        try (PrintStream out= System.out ) {

            // remove the empty string, which is caused by naive launch script.
            List<String> argsx= new ArrayList<>();
            for (String arg : args) {
                if (arg.length() > 0) {
                    argsx.add(arg);
                } else {
                    break;
                }
            }
            args= argsx.toArray( new String[argsx.size()] );
                
            if ( args.length==0 ) {
                printusage();
                System.exit(-1);
            }

            for ( int i=0; i<args.length; i++ ) {
                System.err.println( "arg "+i+": "+ args[i] );
            }
            
            Pattern p= Pattern.compile( args[0] );
            String format= "$0";
            if ( args.length>1 ) {
                format= args[1];
            }
            
            BufferedReader in;
            
            if ( args.length>2 ) {
                in = new BufferedReader( new FileReader( new File( args[2] ) ) );
            } else {
                in = new BufferedReader( new InputStreamReader( System.in ) ); 
            }
            
            String line = in.readLine();
            int linenum = 1;
            int matchnum = 0;
            while ( line!=null ) {
                Matcher m= p.matcher(line);
                if ( m.matches() ) {
                    matchnum++;
                    String outline= format;
                    if ( format.equals("$0") ) {
                        outline= line;
                    } else {
                        String[] ss= outline.split("\\$");
                        StringBuilder outline2= new StringBuilder();
                        outline2.append(ss[0]);
                        for ( int i=1; i<ss.length; i++ ) {
                            String field= ss[i];
                            if ( Character.isDigit(field.charAt(0)) ) {
                                int digit= Integer.parseInt(field.substring(0,1));
                                if ( field.length()>1 && Character.isDigit(field.charAt(1)) ) {
                                    throw new IllegalArgumentException("only 9 fields allowed");
                                }
                                if ( field.length()>1 && field.charAt(1)=='[' ) {
                                    int i2= field.indexOf(']');
                                    String subString= field.substring(2,i2);
                                    String insert= doSubString( m.group(digit), subString );
                                    outline2.append( insert );
                                    outline2.append( field.substring(i2+1) );
                                } else {
                                    outline2.append( m.group(digit) );
                                    outline2.append( field.substring(1) );
                                }
                            } else if ( field.startsWith("{") ) {
                                int i2= field.indexOf('}');
                                String var= field.substring(1,i2);
                                if ( var.equals("LINENUM") ) {
                                    outline2.append( linenum );
                                } else if ( var.equals("MATCHNUM") ) {
                                    outline2.append( matchnum );
                                } else if ( var.equals("NEWLINE") ) {
                                    outline2.append( "\n" );
                                } else {
                                    throw new IllegalArgumentException("LINENUM or MATCHNUM or NEWLINE");
                                }
                                outline2.append( field.substring(i2+1) );
                            }
                        }
                        outline= outline2.toString();
                    }
                    out.println(outline);
                }
                line = in.readLine();
                linenum++;
            }

        }
            
    }
}
