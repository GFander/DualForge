"""Strict reader for official data-get SNBT snapshots; never supplies missing state."""
import re


def parse(text):
    at=0
    def whitespace():
        nonlocal at
        while at<len(text) and text[at].isspace():at+=1
    def quoted():
        nonlocal at
        quote=text[at];at+=1;chars=[]
        while at<len(text):
            ch=text[at];at+=1
            if ch==quote:return ''.join(chars)
            if ch=='\\':
                if at==len(text):raise ValueError('Truncated escape')
                ch=text[at];at+=1
            chars.append(ch)
        raise ValueError('Unterminated string')
    def value():
        nonlocal at
        whitespace()
        if at==len(text):raise ValueError('Missing value')
        if text[at] in '\"\'':return quoted()
        if text[at]=='{':
            at+=1;result={};whitespace()
            while at<len(text) and text[at]!='}':
                whitespace()
                if text[at] in '\"\'':key=quoted()
                else:
                    begin=at
                    while at<len(text) and text[at] not in ':,{}':at+=1
                    key=text[begin:at].strip()
                whitespace()
                if not key or key in result or at==len(text) or text[at]!=':':raise ValueError('Invalid/duplicate key')
                at+=1;result[key]=value();whitespace()
                if at<len(text) and text[at]==',':at+=1
                elif at<len(text) and text[at]=='}':break
                else:raise ValueError('Missing compound delimiter')
            if at==len(text):raise ValueError('Unclosed compound')
            at+=1;return result
        if text[at]=='[':
            at+=1;whitespace();result=[]
            if at+1<len(text) and text[at] in 'BIL' and text[at+1]==';':at+=2
            while at<len(text) and text[at]!=']':
                result.append(value());whitespace()
                if at<len(text) and text[at]==',':at+=1
                elif at<len(text) and text[at]==']':break
                else:raise ValueError('Missing list delimiter')
            if at==len(text):raise ValueError('Unclosed list')
            at+=1;return result
        begin=at
        while at<len(text) and text[at] not in ',]}':at+=1
        token=text[begin:at].strip()
        if not token:raise ValueError('Empty token')
        if re.fullmatch(r'[-+]?\d+[bBsSlL]?',token):return int(token.rstrip('bBsSlL'))
        if re.fullmatch(r'[-+]?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?[fFdD]?',token):return float(token.rstrip('fFdD'))
        return token
    result=value();whitespace()
    if at!=len(text):raise ValueError('Trailing SNBT')
    return result


def furnace(response):
    match=re.search(r'has the following block data: (\{[^\n]+)',response)
    if not match:raise ValueError('No official block data response')
    data=parse(match[1])
    for field in ['BurnTime','CookTime','CookTimeTotal','Items']:
        if field not in data:raise ValueError('Missing actual furnace field '+field)
    items={}
    for item in data['Items']:
        slot=item['Slot']
        if slot in items:raise ValueError('Duplicate item slot')
        items[slot]={'id':item['id'],'count':item['count']}
    return {'burn':data['BurnTime'],'cook':data['CookTime'],'cookTotal':data['CookTimeTotal'],'slots':items,'recipesUsed':data.get('RecipesUsed',{})}
