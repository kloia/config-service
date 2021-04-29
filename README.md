# Distribution Configuration Management

A microservice-based applications managing configuration settings for each service is difficultly.
For the above, you can use spring cloud config server using Git repository configuration storage.

### Architect

![img.png](img.png)


First step,
You have to add @EnableConfigServer annotation in MainClass(ConfigServiceApplication).
In order to create config service, you have to add spring-cloud-config-server and spring-boot-starter-security dependencies in pom.xml.

You can create bootstrap.yml file in resource, then write the following in the bootstrap.yml.

``` 
spring:
  application:
    name: config-service
environment: dev
encrypt:
  key: ENCRYPT
---

spring:
  profiles: dev
  cloud:
    config:
      server:
        git:
          uri: ${HOME}/configuration-poc/configurations
          searchPaths:
            - "{application}/${environment}"
          clone-on-start: false
          basedir: /tmp/tmp-git/
          strict-host-key-checking: false
        prefix: /configs/
server:
  port: 8888
environment: dev 
```

You can create different configuration for each environment.

Every repository can also optionally store config files in sub-directories, and patterns to search for those directories can be specified as `searchPaths`. The following example shows a config:

``` 
  searchPaths:
    - "{application}/${environment}"
```

To change the location of the repository, you can set the spring.cloud.config.server.git.uri configuration property in the Config Server (for example in bootstrap.yml).
If you set it with a file: `prefix`, it should work from a local repository so that you can get started quickly and easily without a server.

If you want to added authentication in config service, there is an example in the configuration package.
The below example code block.

```
@Configuration
@EnableWebSecurity
public class CustomWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().authorizeRequests().anyRequest().authenticated().and().httpBasic();
    }

    @Autowired
    public void configureAuthGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser("user")
                .password(passwordEncoder().encode("123qwe"))
                .roles("USER");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
```

ProfileFilter provides the following: 
Config service filters the incoming request(ex. http://localhost:8888/configs/student/dev. student you directory), goes to the directory-based config path made from the git repository and show config. 

```
 @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String applicationName = findApplicationName(request.getRequestURI());
        if (request.getAttribute(REQUEST_PARAM) != null || StringUtils.isEmpty(applicationName)) {
            endFilter(servletRequest, servletResponse, filterChain);
            return;
        }
        beforeForwarding(request);
        if (applicationName.equals(CONFIG_PREFIX + "/resource/")) {
            prepareResourcePath(servletRequest, servletResponse, request, applicationName);
        } else {
            prepareConfigPath(servletRequest, servletResponse, applicationName);
        }
    }
    
    
    private void prepareResourcePath(ServletRequest servletRequest, ServletResponse servletResponse,
                                     HttpServletRequest request,
                                     String applicationName) throws ServletException, IOException {
        String resourceString = request.getRequestURI().substring(applicationName.length());
        servletRequest.getRequestDispatcher(getResourcePath(resourceString)).forward(servletRequest, servletResponse);
    }
    
    private String getResourcePath(String value) {
        String[] split = value.split(FOLDER_SEPERATOR);
        return new StringBuilder()
                .append(CONFIG_PREFIX)
                .append(FOLDER_SEPERATOR)
                .append(split[0])
                .append(FOLDER_SEPERATOR)
                .append(environment)
                .append(BRANCH_NAME)
                .append(split[1])
                .toString();
    }
```
Above is the code. 
