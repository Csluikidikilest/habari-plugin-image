# Habari Plugin Directory
## Purpose
This is a plugin which permit Habari to process image.

## Configuration
```json
{
  "connectionType": "com.qazima.habari.plugin.image.Plugin",
  "configuration": {
    "grayscaleParameterName": "(grayscale|gs)",
    "heightParameterName": "(height|h)",
    "path": "/var/www/pictures/",
    "rotateParameterName": "(rotate|r)",
    "uri": "(^/img/)(.*)",
    "widthParameterName": "(width|w)"
  }
}
```

