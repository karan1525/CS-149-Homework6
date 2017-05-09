import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

/**
 * A class representing the tester for the child class
 * @author Karan Bhargava
 *
 */
public class ChildTester {

	static final int TOT_PROC = 5;

	public static void main(String[] args) throws IOException, InterruptedException {

		long start_time = System.currentTimeMillis(); //get the current time
		//create 5 pipes, threads and processes
		Pipe[] child_pipes = new Pipe[TOT_PROC]; 
		Thread[] child_thread = new Thread[TOT_PROC];
		Child[] child_processes = new Child[TOT_PROC];

		// A Selector is a Java NIO component which can examine one or more NIO Channel's, 
		// and determine which channels are ready for e.g. reading or writing.
		Selector selector;

		ByteBuffer byte_buffer = ByteBuffer.allocate(128);
		int ready_channels;
		PrintWriter writer = new PrintWriter("output.txt", "UTF-8"); //write to the output.txt file
		selector = Selector.open(); //open the selector

		//create child process with the pipes
		for(int i = 0; i < TOT_PROC; i++) {
			child_pipes[i] = Pipe.open();
			child_pipes[i].source().configureBlocking(false);
			child_pipes[i].source().register(selector, SelectionKey.OP_READ);
			child_processes[i] = new Child(i, start_time, child_pipes[i]);
			child_thread[i] = new Thread(child_processes[i]);
		}

		// start the threads
		for (int i = 0; i < TOT_PROC; i++) {
			child_thread[i].start();
		}

		boolean stop = true;

		while(stop) { //stop the parent process if any of the children have finished

			//if there are no child processes running
			if(isRunning(Arrays.copyOf(child_thread, child_thread.length - 1)) == false) { 
				//interrupt any user input if all threads are dead
				child_thread[child_thread.length - 1].interrupt();

				stop = false;
				System.out.println("\nAll child processes terminated.");
				break;
			}

			ready_channels = selector.select(); //get the channels that are ready for I/O
			if(ready_channels == 0) {  //if there are no channels available
				System.out.println("No channels are ready!");
				continue;
			}

			//a set and an iterator to loop over the pipes
			Set<SelectionKey> selected_keys = selector.selectedKeys();
			Iterator<SelectionKey> key_iterator = selected_keys.iterator();

			while(key_iterator.hasNext()) { //while there is a pipe ready to be read from
				SelectionKey key = key_iterator.next();
				// check whether the current key can be used to established  a remote server.
				if (key.isConnectable()) {
				} else if (key.isAcceptable()) { //check whether the current key was accepted by a ServerSocketChannel.
				} else if (key.isReadable()) { //if channel is valid for I/O

					//get the current time for time stamp and put it in the txt file
					double current_time = (double) ((System.currentTimeMillis() - start_time) / 1000.0);
					String time_stamp = String.format("%06.3f", current_time);
					writer.print("0:" + time_stamp + ", ");
					int bytes_read = ((Pipe.SourceChannel)key.channel()).read(byte_buffer); //get the number of bytes read
					byte_buffer.flip(); //make the buffer ready to be read

					char previous = ' ';
					while(byte_buffer.hasRemaining()) { //while there's elements in the buffer
						char current = (char) byte_buffer.get(); //get the current char
						//if there's multiple messages, reprint the time stamp
						if (previous == '\n') {
							writer.print("0:" + time_stamp + ", ");
						}

						writer.print(current);
						previous = current;
					}

					byte_buffer.clear();
	
				} else if(key.isWritable()) { //remove key if channel is ready for writing
				}
				
				byte_buffer.clear();
				key_iterator.remove();
			}
		}
		System.out.println("Parent process terminated");
		writer.close();
		System.exit(0);
	}

	/**
	 * A helper method to check if the thread is still running
	 * @param arr - the array we're working with
	 * @return - true if running; else false
	 */
	static boolean isRunning(Thread[] arr) {
		for (int i = 0; i < arr.length; i++) {
			if(arr[i].isAlive()) {
				return true;
			}
		}

		return false;
	}
}
