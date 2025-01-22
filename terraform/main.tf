# Configure the Azure provider
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
}

provider "azurerm" {
  features {}
}

# Create a resource group
resource "azurerm_resource_group" "volunteer_rg" {
  name     = "volunteer-auth-rg"
  location = "francecentral"
}

# Create Service Plan
resource "azurerm_service_plan" "volunteer_plan" {
  name                = "volunteer-auth-plan"
  location            = azurerm_resource_group.volunteer_rg.location
  resource_group_name = azurerm_resource_group.volunteer_rg.name
  os_type            = "Linux"
  sku_name           = "B1"
}

# Create MySQL Flexible Server
resource "azurerm_mysql_flexible_server" "volunteer_mysql" {
  name                   = "volunteer-auth-mysql-1"
  location               = azurerm_resource_group.volunteer_rg.location
  resource_group_name    = azurerm_resource_group.volunteer_rg.name
  administrator_login    = "mysqladmin"
  administrator_password = "P@ssw0rd123!"
  sku_name              = "B_Standard_B1ms"
  version               = "8.0.21"
  zone                  = "1"

  storage {
    size_gb = 20
  }

  depends_on = [azurerm_resource_group.volunteer_rg]
}

# Create MySQL Database
resource "azurerm_mysql_flexible_database" "auth_db" {
  name                = "auth_db"
  resource_group_name = azurerm_resource_group.volunteer_rg.name
  server_name         = azurerm_mysql_flexible_server.volunteer_mysql.name
  charset             = "utf8"
  collation          = "utf8_unicode_ci"

  depends_on = [azurerm_mysql_flexible_server.volunteer_mysql]
}

# Create Web App
resource "azurerm_linux_web_app" "auth_app" {
  name                = "volunteer-auth-app-1"  # Modified name
  location            = azurerm_resource_group.volunteer_rg.location
  resource_group_name = azurerm_resource_group.volunteer_rg.name
  service_plan_id     = azurerm_service_plan.volunteer_plan.id

  site_config {
    application_stack {
      java_version          = "17"
      java_server          = "JAVA"
      java_server_version  = "17"
    }
    always_on = true
  }

  app_settings = {
    "WEBSITES_PORT"              = "8080"
    "SPRING_PROFILES_ACTIVE"     = "prod"
    "SPRING_DATASOURCE_URL"      = "jdbc:mysql://${azurerm_mysql_flexible_server.volunteer_mysql.fqdn}:3306/auth_db?useSSL=true&requireSSL=false"
    "SPRING_DATASOURCE_USERNAME" = azurerm_mysql_flexible_server.volunteer_mysql.administrator_login
    "SPRING_DATASOURCE_PASSWORD" = azurerm_mysql_flexible_server.volunteer_mysql.administrator_password
    "JWT_SECRET"                 = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437"
    "JWT_EXPIRATION"             = "86400000"
  }

  depends_on = [azurerm_service_plan.volunteer_plan]
}

# Configure MySQL Firewall after server creation
resource "azurerm_mysql_flexible_server_firewall_rule" "allow_azure" {
  name                = "allow-azure"
  resource_group_name = azurerm_resource_group.volunteer_rg.name
  server_name         = azurerm_mysql_flexible_server.volunteer_mysql.name
  start_ip_address    = "0.0.0.0"
  end_ip_address      = "255.255.255.255"

  depends_on = [azurerm_mysql_flexible_server.volunteer_mysql]
}

# Output values
output "webapp_url" {
  value = azurerm_linux_web_app.auth_app.default_hostname
}

output "mysql_server_fqdn" {
  value = azurerm_mysql_flexible_server.volunteer_mysql.fqdn
}