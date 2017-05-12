package au.org.aekos.api.controller;

import java.util.Locale;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import com.github.mkopylec.recaptcha.validation.RecaptchaValidator;
import com.github.mkopylec.recaptcha.validation.ValidationResult;

import au.org.aekos.api.Constants;
import au.org.aekos.api.service.auth.AekosApiAuthKey;
import au.org.aekos.api.service.auth.AekosApiAuthKey.InvalidKeyException;
import au.org.aekos.api.service.auth.AekosUserDetailsService;
import au.org.aekos.api.service.auth.AuthStorageService;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;


@RestController
@RequestMapping(Constants.V1_0)
public class SignupController {

	private static final Logger logger = LoggerFactory.getLogger(SignupController.class);

	@Autowired
	private AuthStorageService authStorageService;
	
	@Autowired
    private JavaMailSender javaMailSender;
	
    @Autowired
    private SpringTemplateEngine templateEngine;
	
    @Autowired
    private RecaptchaValidator recaptchaValidator;

    /* Params to use in the templated email */
	@Value("${aekos-api.spring.mail.from}")
	private String from;

	@Value("${aekos-api.spring.mail.cc}")
	private String cc;

	@Value("${aekos-api.spring.mail.subject}")
	private String subject;

	@Value("${aekos-api.spring.mail.bodyText}")
	private String bodyText;

	@Value("${aekos-api.spring.mail.logo}")
	private String logo;

    private final AekosUserDetailsService aekosUserDetailsService;

    @Autowired
    public SignupController(AekosUserDetailsService aekosUserDetailsService) {
       this.aekosUserDetailsService = aekosUserDetailsService;
    }
    
    @RequestMapping(path = "/signup", method = RequestMethod.POST)
    @ApiIgnore
    @ApiOperation(value = "", notes = "This method is called to validate the captcha on signup.", tags="Signup")
    public ModelAndView validateCaptcha(HttpServletRequest request) {
        ValidationResult result = recaptchaValidator.validate(request);
        if (result.isSuccess()) {
        	logger.debug("Authenticated");
        	
        	String emailAddress = request.getParameter("email");
        	logger.info("emailAddress = " + emailAddress);

        	String keyUUIDString = UUID.randomUUID().toString();
        	AekosApiAuthKey key;
			try {
				key = new AekosApiAuthKey(keyUUIDString);
	        	authStorageService.storeNewKey(emailAddress, key, AuthStorageService.SignupMethod.EMAIL);
	        	
	            final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
	            MimeMessageHelper message;
				try {
					message = new MimeMessageHelper(mimeMessage,true, "UTF-8");
		        	message.setTo(emailAddress);
		            message.setFrom(from);
		        	message.setCc(cc);
		            message.setSubject(subject);
		        
		            final Context ctx = new Context(new Locale("en-AU"));
		            ctx.setVariable("subject", subject);
		            ctx.setVariable("bodyText", bodyText);
		            ctx.setVariable("keyUUIDString", keyUUIDString);

		            final String htmlContent = this.templateEngine.process( "email-signup", ctx);
		            message.setText(htmlContent, true);

		            javaMailSender.send(mimeMessage);
				} catch (MessagingException e) {
					logger.error("Could not send the templated email message - " + e.getMessage());
		        	return new ModelAndView("redirect:/signedup.html?status=internalfailure");
				}
			
			} catch (InvalidKeyException e) {
				logger.error("Could not create the API key - " + e.getMessage());
	        	return new ModelAndView("redirect:/signedup.html?status=internalfailure");
			}
        	
    		return new ModelAndView("redirect:/signedup.html?status=ok");
        } else {
			logger.error("Captcha failed - maybe a bot?");
        	return new ModelAndView("redirect:/signedup.html?status=captchafailure");
        }
    }
    
    //
    // Test service - potentially dump this.
    //
    @RequestMapping(path = "/exists/{username}", method = RequestMethod.GET)
    public boolean userExists(@PathVariable("username") String username ) {
        return aekosUserDetailsService.userExists(username);
    }
}