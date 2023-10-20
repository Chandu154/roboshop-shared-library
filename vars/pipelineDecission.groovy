#!groovy


// below function getting inputs from configMap

def decidePipeline(Map configMap){
    application = configMap.get("application")
    // here we are getting nodeJSVM
    switch(application) {
        case 'nodeJSVM':
            echo "appication is NodeJS and VM based"
            //nodeJSVMCI(configMap)
            break
        case 'JavaVM':
            javaVMCI(configMap)
            break
        default:
            error "Un recongnised appilication"
            break        
    }
}
