# ADR-002 – Java 21, Spring Boot und Maven

- **Status:** Akzeptiert, Umsetzung ausstehend
- **Datum:** 2026-09-30
- **Bezug:** EPIC-01, EPIC-03, EPIC-14–16, [Technologien](../TECHNOLOGIES.md)

## Kontext

Das Backend muss verständliche Fachlogik, dauerhafte Daten, HTTP-Schnittstellen, Rechteprüfung und zuverlässige Tests verbinden. Die Technologieauswahl soll den Ausbildungsfokus unterstützen und einen reproduzierbaren Build ermöglichen.

## Entscheidung

Wir verwenden Java 21 und Spring Boot für eine synchrone HTTP-Anwendung. Spring Security übernimmt Anmeldung und Sessions sowie integrierte Schutzmechanismen. Spring Boot Actuator liefert begrenzte Betriebsinformationen. Maven mit versioniertem Maven Wrapper baut und prüft die Anwendung.

JUnit, Spring Boot Test und Testcontainers bilden die Backend-Testbasis. Die bisher genannte JUnit-5-Vorgabe wird zusammen mit der konkreten gepflegten Boot-Version geprüft; ein erforderlicher Hauptversionswechsel wird sichtbar dokumentiert. Konkrete Bibliotheksversionen werden in M0 festgeschrieben, nicht als bereits getestet vorgetäuscht.

Spring MVC und Spring Data JPA/Hibernate sind ergänzende Arbeitsannahmen für HTTP und Persistenz. Ein reaktiver Stack ist für die vorgesehenen normalen Datenbankanfragen nicht erforderlich. Flyway verwaltet das Schema unabhängig vom ORM.

## Gründe und Alternativen

Spring Boot bündelt die Integration vieler benötigter Java-Bausteine. Maven beschreibt Abhängigkeiten und Build nachvollziehbar; der Wrapper reduziert Unterschiede zwischen Rechnern. Java 21 ist ausdrücklich vorgegeben.

Node.js- oder Python-Backends könnten den fachlichen Umfang ebenfalls umsetzen, würden jedoch vom gemeinsam festgelegten Lernstack abweichen. Java ohne Spring hätte weniger Framework-Konventionen, erforderte aber mehr eigene Sicherheits-, Konfigurations- und Integrationsarbeit. Gradle bleibt eine mögliche Alternative, bietet für dieses Projekt jedoch keinen notwendigen Vorteil gegenüber dem vereinbarten Maven.

## Konsequenzen

Azubis müssen Begriffe wie Dependency Injection, Controller, Service und Repository einordnen lernen. Framework-Standards werden genutzt, aber nicht unbesehen angenommen: Rollen, Objektberechtigungen, Session-/CSRF-Verhalten und Konfiguration benötigen Tests.

Das Backend läuft zunächst als eine Instanz mit Sessions im Prozess. Ein Neustart kann neue Anmeldung erfordern. JWT, externe Identitätsanbieter und verteilte Session-Infrastruktur sind nicht Teil der ersten Umsetzung.

## Überprüfung

M0 dokumentiert eine mit Java 21 kompatible, gepflegte Boot-Version und die dazu passenden Test-, Security-, Datenbank- und OpenAPI-Bibliotheken. Der Maven-Build einschließlich echter PostgreSQL-Integrationstests muss reproduzierbar laufen. [Offizielle Spring-Boot-Systemvoraussetzungen](https://docs.spring.io/spring-boot/system-requirements.html)
