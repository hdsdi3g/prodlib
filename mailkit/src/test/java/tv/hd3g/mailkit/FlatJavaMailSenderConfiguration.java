package tv.hd3g.mailkit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import tv.hd3g.mailkit.utility.FlatJavaMailSender;

@Configuration
public class FlatJavaMailSenderConfiguration {

	@Bean
	@Primary
	FlatJavaMailSender flatJavaMailSender() {
		return new FlatJavaMailSender(true);
	}

}
