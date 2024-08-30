
## Example usage of Track Executive Time

```
@GetMapping("/health/ping")
@TrackExecutionTime
public ResponseEntity<List<String>> healthCheck()
{
    return new ResponseEntity<>(HttpStatus.OK);
}
```