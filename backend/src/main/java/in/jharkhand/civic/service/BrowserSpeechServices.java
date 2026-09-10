package in.jharkhand.civic.service; import in.jharkhand.civic.port.*; import org.springframework.stereotype.Component;
@Component("browserSpeechToTextService") class BrowserSpeechToTextService implements SpeechToTextService{public String providerName(){return "Browser Web Speech API";}}
@Component("browserTextToSpeechService") class BrowserTextToSpeechService implements TextToSpeechService{public String providerName(){return "Browser Speech Synthesis API";}}
