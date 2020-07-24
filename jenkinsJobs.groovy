job("j-job1"){
        description("this job will copy the file in you os version and push image to docker hub")
        scm {
                 github('cr7-ayush/autodockerfile' , 'master')
             }
        triggers {
                scm("* * * * *")
                
        }

        steps {
        shell('''sudo cp * /html/
sudo docker build -t ayushkumar13/http:latest .
sudo docker push ayushkumar13/http''')
      }
}


job("j-job2"){
        description("this will create deployment for website and expose deployment")
        
        triggers {
        upstream {
    upstreamProjects("j-job1")
    threshold("Fail")
        }
        }

        steps {
        shell('''if sudo kubectl get deployment | grep myweb
then
echo " updating"
else
sudo kubectl create deployment myweb --image=ayushkumar13/http
sudo kubectl autoscale deployment myweb --min=10 --max=15 --cpu-percent=80
fi
if sudo kubectl get deployment -o wide | grep latest
then 
sudo kubectl set image deployment myweb http=ayushkumar13/http
else
sudo kubectl set image deployment myweb http=ayushkumar13/http:latest
fi
if sudo kubectl get service | grep myweb
then 
echo "service exist"
else
sudo kubectl expose deployment myweb --port=80 --type=NodePort
fi ''')
      }
}


job("j-job3") {
  description ("It will test if pod is running else send a mail")
  
  triggers {
    upstream('j-job2', 'SUCCESS')
  }
  steps {
    shell('''if sudo kubectl get deployment | grep myweb
then
echo "send to production"
else
echo "sending back to developer"
exit 1
fi''')
  }
  publishers {
    extendedEmail {
      contentType('text/html')
      triggers {
        success{
          attachBuildLog(true)
          subject('Build successfull')
          content('The build was successful and deployment was done.')
          recipientList('ayushkumar.tiwari@gmail.com')
        }
        failure{
          attachBuildLog(true)
          subject('Failed build')
          content('The build was failed')
          recipientList('ayushkumar.tiwari@gmail.com')
        }
      }
    }
  }
}


buildPipelineView('j-jobs-view') {
  filterBuildQueue(true)
  filterExecutors(false)
  title('j-jobs-view')
  displayedBuilds(1)
  selectedJob('j-job1')
  alwaysAllowManualTrigger(false)
  showPipelineParameters(true)
  refreshFrequency(1)
}