package th.go.etda.sarabun.pdf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sarabun PDF Generation API - Main Application
 * 
 * แปลงมาจาก: ETDA.SarabunMultitenant.Api/Program.cs
 * การเปลี่ยนแปลง:
 * - ใช้ Spring Boot แทน ASP.NET Core
 * - ใช้ Apache PDFBox แทน iText (เพื่อหลีกเลี่ยง license issues)
 * - รองรับ Windows และ Linux
 * 
 * @author Migrated from .NET to Java
 * @version 1.0.0
 */
@SpringBootApplication
public class SarabunPdfApplication {

	public static void main(String[] args) {
		SpringApplication.run(SarabunPdfApplication.class, args);
	}
}
