# Supported Fields

### int, float, boolean (chosen randomly)
```
workloads:
    ....
    fields:
      - name: some_integer # Names are flexible 
        type: int          # Types are not
      - name: some_float
        type: float
      - name: pass_or_fail
        type: boolean
   ...
```
### int, float (with a given range)
```
workloads:
    ....
    fields:
      - name: account_number
        type: int
        range: 0,9999999
      - name: balance
        type: float
        range: 0,999999
   ...
```
### Names
```
workloads:
    ....
    fields:
      - name: customer_name
        type: full_name
      - name: name.first 
        type: first_name
      - name: name.last
        type: last_name
      - name: department
        type: group
      - name: team
        type: team_name
        
        
   ...
```
### Addresses
```
workloads:
    ....
    fields:
      - name: address
        type: full_address
      - name: street
        type: street_address
      - name: city
        type: city
      - name: state
        type: state
      - name: zip_code
        type: zipcode
   ...
```
### Special Numbers
```
workloads:
    ....
    fields:
      - name: cc_no
        type: credit_card_number
      - name: phone
        type: phone_number
      - name: social_security_number
        type: ssn
      - name: state
        type: state
      - name: token
        type: uuid
      - name: product_name
        type: product_name
      - name: random_hash
        type: hash
   ...
```
### Random From lists
```
workloads:
    ....
    fields:
      - name: name
        type: random_string_from_list
        custom_list: steve,john,aj,baz
      - name: number
        type: random_integer_from_list
        custom_list: 12,19,15
      - name: float
        type: random_float_from_list
        custom_list: 3.14,1.00,0.99
      - name: long
        type: random_long_from_list
        custom_list: 10213123,1231515145,12431534999
```
### Misc
```
workloads:
    ....
    fields:
      - name: ip_address
        type: ipv4
      - name: chuck_norris_fact
        type: random_cn_fact
      - name: character
        type: random_got_character  
      - name: job
        type: random_occupation
      - name: blank_field
        type: empty  
      - name: target_file_name
        type: path
      - name: servername
        type: hostname  (List based on ancient gods)
      - name: application_name
        type: appname
      - name: random_url
        type: url
      - name: mac
        type: mac_address
      - name: email_address
        type: email
      - name: domain_name
        type: domain
      - name: transaction_date
        type: date
      - name: tz
        type: timezone
```
### Constants
```
workloads:
....
 fields:
      - name: constant_string
        value: something that shouldn't change
```
